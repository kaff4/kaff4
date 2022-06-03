package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.io.Sha256FileSystemFactory
import com.github.nava2.aff4.io.repeatByteString
import com.github.nava2.aff4.model.rdf.CompressionMethod
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryModule
import com.github.nava2.aff4.streams.WritingModule
import com.github.nava2.aff4.streams.compression.SnappyCompression
import com.github.nava2.configuration.TestConfigProviderModule
import com.github.nava2.test.GuiceTestRule
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Timeout
import okio.buffer
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import javax.inject.Inject

class Aff4ImageStreamSinkTest {
  @get:Rule
  var tempDirectoryRule: TemporaryFolder = TemporaryFolder()

  private val tempDirectory: Path
    get() {
      tempDirectoryRule.create()
      return tempDirectoryRule.root.toOkioPath()
    }

  @get:Rule
  var rule = GuiceTestRule(
    TestConfigProviderModule,
    Aff4CoreModule,
    MemoryRdfRepositoryModule,
    WritingModule(tempDirectory),
  )

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var bevyFactory: Bevy.Factory

  @Inject
  private lateinit var sha256FileSystemFactory: Sha256FileSystemFactory

  @Inject
  private lateinit var snappyCompression: SnappyCompression

  private val imageFileSystem: FileSystem by lazy { sha256FileSystemFactory.create(tempDirectory) }

  private val dataBuffer = Buffer()

  @After
  fun tearDown() {
    dataBuffer.clear()
  }

  @Test
  fun `create image stream`() {
    val chunkSize = 5
    val chunksInSegment = 2
    val containerArn = valueFactory.createIRI("aff4://99cc4380-308f-4235-838c-e20a8898ad00")
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

    val writtenImageStream = Aff4ImageStreamSink(
      bevyFactory = bevyFactory,
      outputFileSystem = imageFileSystem,
      imageStream = imageStream,
      blockHashTypes = listOf(),
      timeout = Timeout.NONE,
    ).run {
      buffer().use { sink ->
        sink.write(content)
      }

      this.imageStream
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
  }

  @Test
  fun `write across bevy boundary with chunk buffer partially filled`() {
    val chunkSize = 5
    val chunksInSegment = 2
    val containerArn = valueFactory.createIRI("aff4://99cc4380-308f-4235-838c-e20a8898ad00")
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

    val writtenImageStream = Aff4ImageStreamSink(
      bevyFactory = bevyFactory,
      outputFileSystem = imageFileSystem,
      imageStream = imageStream,
      blockHashTypes = listOf(),
      timeout = Timeout.NONE,
    ).use { imageStreamSink ->
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
  }

  @Test
  fun `create snappy bevy`() {
    val content = 0.repeatByteString(100)

    val chunkSize = 40
    val chunksInSegment = 1
    val containerArn = valueFactory.createIRI("aff4://bb362b22-649c-494b-923f-e4ed0c5afef4")
    val imageStream = ImageStream(
      arn = valueFactory.createIRI("aff4://99cc4380-308f-4235-838c-e20a8898ad00"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = content.size.toLong(),
      compressionMethod = snappyCompression,
      stored = containerArn,
      linearHashes = listOf(HashType.SHA256, HashType.MD5).map { it.value(ByteString.EMPTY) },
    )

    val writtenImageStream = Aff4ImageStreamSink(
      bevyFactory = bevyFactory,
      outputFileSystem = imageFileSystem,
      imageStream = imageStream,
      blockHashTypes = listOf(),
      timeout = Timeout.NONE,
    ).run {
      buffer().use { sink ->
        sink.write(content)
      }

      this.imageStream
    }

    val md5LinearHash = HashType.MD5.value("6d0bb00954ceb7fbee436bb55a8397a9".decodeHex())
    val sha256LinearHash = HashType.SHA256.value(
      "cd00e292c5970d3c5e2f0ffa5171e555bc46bfc4faddfb4a418b6840b86e79a3".decodeHex(),
    )

    assertThat(writtenImageStream)
      .isEqualTo(
        imageStream.copy(
          size = content.size.toLong(),
          linearHashes = listOf(sha256LinearHash, md5LinearHash),
        )
      )
  }
}
