package net.navatwo.kaff4.streams.image_stream

import com.google.inject.util.Modules
import jakarta.inject.Inject
import net.navatwo.kaff4.Aff4BaseStreamModule
import net.navatwo.kaff4.UsingTemporary
import net.navatwo.kaff4.container.Aff4ContainerBuilder
import net.navatwo.kaff4.container.RealAff4ContainerBuilder
import net.navatwo.kaff4.io.decode
import net.navatwo.kaff4.io.repeatByteString
import net.navatwo.kaff4.model.Aff4ImageOpener
import net.navatwo.kaff4.model.dialect.DefaultToolDialect
import net.navatwo.kaff4.model.dialect.ToolDialect
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.CompressionMethod
import net.navatwo.kaff4.model.rdf.Hash
import net.navatwo.kaff4.model.rdf.HashType
import net.navatwo.kaff4.model.rdf.ImageStream
import net.navatwo.kaff4.model.rdf.None
import net.navatwo.kaff4.model.rdf.createArn
import net.navatwo.kaff4.rdf.MemoryRdfRepositoryFeatureModule
import net.navatwo.kaff4.streams.TestAff4ContainerBuilderModule
import net.navatwo.kaff4.streams.compression.Aff4SnappyFeatureModule
import net.navatwo.kaff4.streams.compression.SnappyCompression
import net.navatwo.kaff4.streams.compression.deflate.Aff4DeflateFeatureModule
import net.navatwo.kaff4.streams.compression.deflate.DeflateCompression
import net.navatwo.kaff4.streams.compression.lz4.Aff4Lz4FeatureModule
import net.navatwo.kaff4.streams.compression.lz4.Lz4Compression
import net.navatwo.test.GuiceModule
import net.navatwo.test.OffTestThreadExecutor
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class Aff4ImageStreamSinkTest {
  @GuiceModule
  val module = Modules.combine(
    TestAff4ContainerBuilderModule,
    Aff4BaseStreamModule,
    MemoryRdfRepositoryFeatureModule,
    Aff4SnappyFeatureModule,
    Aff4Lz4FeatureModule,
    Aff4DeflateFeatureModule,
  )

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var snappyCompression: SnappyCompression

  @Inject
  private lateinit var lz4Compression: Lz4Compression

  @Inject
  private lateinit var deflateCompression: DeflateCompression

  @Inject
  private lateinit var aff4ImageOpener: Aff4ImageOpener

  @Inject
  private lateinit var aff4ContainerBuilderFactory: Aff4ContainerBuilder.Factory

  @Inject
  private lateinit var offTestThreadExecutor: OffTestThreadExecutor

  @Inject
  @field:DefaultToolDialect
  private lateinit var toolDialect: ToolDialect

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
        toolDialect = toolDialect,
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
      linearHashes = listOf(HashType.SHA256, HashType.MD5).map { it.value(ByteString.EMPTY) }.toSet(),
    )

    val content = "abcdefghijklmno".encodeUtf8()

    val writtenImageStream = aff4ContainerBuilder.createImageStream(imageStream, listOf())
      .use { imageStreamSink ->
        imageStreamSink.buffer().use { it.write(content) }

        imageStreamSink.imageStream
      }

    val md5LinearHash = Hash.Md5.decode("8a7319dbf6544a7422c9e25452580ea5")
    val sha256LinearHash = Hash.Sha256.decode("41c7760c50efde99bf574ed8fffc7a6dd3405d546d3da929b214c8945acf8a97")

    assertThat(writtenImageStream)
      .isEqualTo(
        imageStream.copy(
          size = content.size.toLong(),
          linearHashes = setOf(sha256LinearHash, md5LinearHash),
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
      linearHashes = listOf(HashType.SHA256, HashType.MD5).map { it.value(ByteString.EMPTY) }.toSet(),
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

    val md5LinearHash = Hash.Md5.decode("8a7319dbf6544a7422c9e25452580ea5")
    val sha256LinearHash = Hash.Sha256.decode("41c7760c50efde99bf574ed8fffc7a6dd3405d546d3da929b214c8945acf8a97")

    assertThat(writtenImageStream)
      .isEqualTo(
        imageStream.copy(
          size = content.size.toLong(),
          linearHashes = setOf(sha256LinearHash, md5LinearHash),
        )
      )

    verifyWrittenStream(writtenImageStream)
  }

  @ParameterizedTest(name = "create ({0}) compressed bevy")
  @ValueSource(
    strings = [
      SnappyCompression.IDENTIFIER,
      Lz4Compression.IDENTIFIER,
      DeflateCompression.IDENTIFIER,
    ]
  )
  fun `create compressed bevy`(identifier: String) {
    val compressionMethod = when (identifier) {
      SnappyCompression.IDENTIFIER -> snappyCompression
      Lz4Compression.IDENTIFIER -> lz4Compression
      DeflateCompression.IDENTIFIER -> deflateCompression
      else -> error("unknown identifier")
    }

    val content = Buffer().use { buffer ->
      for (i in 0..10) {
        buffer.write(i.repeatByteString(1024 * i))
      }
      buffer.readByteString()
    }

    val chunkSize = 512
    val chunksInSegment = 1
    val imageStream = ImageStream(
      arn = valueFactory.createIRI("aff4://99cc4380-308f-4235-838c-e20a8898ad00"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = content.size.toLong(),
      compressionMethod = compressionMethod,
      stored = aff4ContainerBuilder.containerArn,
      linearHashes = listOf(HashType.SHA256, HashType.MD5).map { it.value(ByteString.EMPTY) }.toSet(),
    )

    val writtenImageStream = aff4ContainerBuilder.createImageStream(imageStream, listOf())
      .use { imageStreamSink ->
        imageStreamSink.buffer().use { it.write(content) }

        imageStreamSink.imageStream
      }

    val md5LinearHash = Hash.Md5.decode("9b9e507936c06ea38b3787fef8e9cd49")
    val sha256LinearHash = Hash.Sha256.decode("4ba9a822ab26251d8ae55840bc3194af4203ca42fdd7e29f9a681877dccaaa9e")

    assertThat(writtenImageStream)
      .usingRecursiveComparison()
      .isEqualTo(
        imageStream.copy(
          size = content.size.toLong(),
          linearHashes = setOf(sha256LinearHash, md5LinearHash),
        )
      )

    verifyWrittenStream(writtenImageStream)
  }

  private fun verifyWrittenStream(writtenImageStream: ImageStream) {
    aff4ContainerBuilder.buildIntoDirectory(outputFileSystem, ".".toPath())

    offTestThreadExecutor.synchronous {
      aff4ImageOpener.open(outputFileSystem, ".".toPath()) { container ->
        val openedImageStream =
          container.streamOpener.openStream(writtenImageStream.arn) as Aff4ImageStreamSourceProvider
        assertThat(openedImageStream.imageStream).isEqualTo(writtenImageStream)

        openedImageStream.verify(container.aff4Model)
      }
    }
  }
}
