package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.Aff4BaseStreamModule
import com.github.nava2.aff4.UsingTemporary
import com.github.nava2.aff4.container.Aff4ContainerBuilder
import com.github.nava2.aff4.container.RealAff4ContainerBuilder
import com.github.nava2.aff4.io.repeatByteString
import com.github.nava2.aff4.model.Aff4ContainerOpener
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.CompressionMethod
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.None
import com.github.nava2.aff4.model.rdf.createArn
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryPlugin
import com.github.nava2.aff4.streams.TestAff4ContainerBuilderModule
import com.github.nava2.aff4.streams.compression.Aff4SnappyPlugin
import com.github.nava2.aff4.streams.compression.SnappyCompression
import com.github.nava2.test.GuiceModule
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Inject

class Aff4ImageStreamSinkTest {

  @GuiceModule
  val modules = listOf(
    TestAff4ContainerBuilderModule,
    Aff4BaseStreamModule,
    MemoryRdfRepositoryPlugin,
    Aff4SnappyPlugin,
  )

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var snappyCompression: SnappyCompression

  @Inject
  private lateinit var aff4ContainerOpener: Aff4ContainerOpener

  @Inject
  private lateinit var aff4ContainerBuilderFactory: Aff4ContainerBuilder.Factory

  @UsingTemporary
  private lateinit var outputFileSystem: FileSystem

  @UsingTemporary(useSha256 = true)
  private lateinit var imageFileSystem: FileSystem

  private val dataBuffer = Buffer()

  private lateinit var containerArn: Aff4Arn
  private lateinit var aff4ContainerBuilder: RealAff4ContainerBuilder

  @BeforeEach
  fun setup() {
    containerArn = valueFactory.createArn("aff4://99cc4380-308f-4235-838c-e20a8898ad00")
    aff4ContainerBuilder = aff4ContainerBuilderFactory.create(
      Aff4ContainerBuilder.Context(
        temporaryFileSystem = imageFileSystem,
        arn = containerArn,
      ),
    ) as RealAff4ContainerBuilder
  }

  @AfterEach
  fun tearDown() {
    dataBuffer.clear()

    aff4ContainerBuilder.close()
  }

  @Test
  fun `create image stream`() {
    val chunkSize = 5
    val chunksInSegment = 2
    val imageStream = ImageStream(
      arn = valueFactory.createIRI("aff4://bb362b22-649c-494b-923f-e4ed0c5afef4"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = Long.MAX_VALUE,
      compressionMethod = CompressionMethod.None,
      stored = containerArn,
      linearHashes = listOf(HashType.SHA256, HashType.MD5).map { it.value(ByteString.EMPTY) },
    )

    val content = "abcdefghijklmno".encodeUtf8()

    val writtenImageStream = aff4ContainerBuilder.createImageStream(imageStream, listOf())
      .use { imageStreamSink ->
        imageStreamSink.buffer().use { it.write(content) }

        imageStreamSink.imageStream
      }

    val md5LinearHash = HashType.MD5.value("8a7319dbf6544a7422c9e25452580ea5".decodeHex())
    val sha256LinearHash = HashType.SHA256.value(
      "41c7760c50efde99bf574ed8fffc7a6dd3405d546d3da929b214c8945acf8a97".decodeHex(),
    )

    assertThat(writtenImageStream)
      .isEqualTo(
        imageStream.copy(
          size = content.size.toLong(),
          linearHashes = listOf(sha256LinearHash, md5LinearHash),
        )
      )

    verifyWrittenStream(writtenImageStream)
  }

  @Test
  fun `write across bevy boundary with chunk buffer partially filled`() {
    val chunkSize = 5
    val chunksInSegment = 2
    val imageStream = ImageStream(
      arn = valueFactory.createIRI("aff4://bb362b22-649c-494b-923f-e4ed0c5afef4"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = Long.MAX_VALUE,
      compressionMethod = CompressionMethod.None,
      stored = containerArn,
      linearHashes = listOf(HashType.SHA256, HashType.MD5).map { it.value(ByteString.EMPTY) },
    )

    val content = "abcdefghijklmno".encodeUtf8()

    val writtenImageStream = aff4ContainerBuilder.createImageStream(imageStream, listOf())
      .use { imageStreamSink ->
        dataBuffer.write(content)

        // Write enough that we cross the chunk boundary but leave room for the bevy in a second write
        imageStreamSink.write(dataBuffer, chunkSize * chunksInSegment - 3.toLong())

        // now finish the content
        imageStreamSink.write(dataBuffer, dataBuffer.size)

        imageStreamSink.close()

        imageStreamSink.imageStream
      }

    val md5LinearHash = HashType.MD5.value("8a7319dbf6544a7422c9e25452580ea5".decodeHex())
    val sha256LinearHash = HashType.SHA256.value(
      "41c7760c50efde99bf574ed8fffc7a6dd3405d546d3da929b214c8945acf8a97".decodeHex(),
    )

    assertThat(writtenImageStream)
      .isEqualTo(
        imageStream.copy(
          size = content.size.toLong(),
          linearHashes = listOf(sha256LinearHash, md5LinearHash),
        )
      )

    verifyWrittenStream(writtenImageStream)
  }

  @Test
  fun `create snappy bevy`() {
    val content = 0.repeatByteString(100)

    val chunkSize = 40
    val chunksInSegment = 1
    val imageStream = ImageStream(
      arn = valueFactory.createIRI("aff4://99cc4380-308f-4235-838c-e20a8898ad00"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = content.size.toLong(),
      compressionMethod = snappyCompression,
      stored = aff4ContainerBuilder.arn,
      linearHashes = listOf(HashType.SHA256, HashType.MD5).map { it.value(ByteString.EMPTY) },
    )

    val writtenImageStream = aff4ContainerBuilder.createImageStream(imageStream, listOf())
      .use { imageStreamSink ->
        imageStreamSink.buffer().use { it.write(content) }

        imageStreamSink.imageStream
      }

    val md5LinearHash = HashType.MD5.value("6d0bb00954ceb7fbee436bb55a8397a9".decodeHex())
    val sha256LinearHash = HashType.SHA256.value(
      "cd00e292c5970d3c5e2f0ffa5171e555bc46bfc4faddfb4a418b6840b86e79a3".decodeHex(),
    )

    assertThat(writtenImageStream)
      .usingRecursiveComparison()
      .isEqualTo(
        imageStream.copy(
          size = content.size.toLong(),
          linearHashes = listOf(sha256LinearHash, md5LinearHash),
        )
      )

    verifyWrittenStream(writtenImageStream)
  }

  private fun verifyWrittenStream(writtenImageStream: ImageStream) {
    aff4ContainerBuilder.buildIntoDirectory(outputFileSystem, ".".toPath())

    aff4ContainerOpener.open(outputFileSystem, ".".toPath()) { container ->
      val openedImageStream = container.streamOpener.openStream(writtenImageStream.arn) as Aff4ImageStreamSourceProvider
      assertThat(openedImageStream.imageStream).isEqualTo(writtenImageStream)

      openedImageStream.verify(container.aff4Model)
    }
  }
}
