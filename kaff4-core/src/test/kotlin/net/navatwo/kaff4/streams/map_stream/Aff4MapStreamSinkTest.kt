package net.navatwo.kaff4.streams.map_stream

import com.google.inject.util.Modules
import net.navatwo.kaff4.Aff4BaseStreamModule
import net.navatwo.kaff4.TestActionScopeModule
import net.navatwo.kaff4.UsingTemporary
import net.navatwo.kaff4.container.Aff4ContainerBuilder
import net.navatwo.kaff4.container.RealAff4ContainerBuilder
import net.navatwo.kaff4.io.Sha256FileSystemFactory
import net.navatwo.kaff4.io.doesNotExist
import net.navatwo.kaff4.io.exists
import net.navatwo.kaff4.io.md5
import net.navatwo.kaff4.io.relativeTo
import net.navatwo.kaff4.io.repeatByteString
import net.navatwo.kaff4.model.Aff4ImageOpener
import net.navatwo.kaff4.model.dialect.DefaultToolDialect
import net.navatwo.kaff4.model.dialect.ToolDialect
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.CompressionMethod
import net.navatwo.kaff4.model.rdf.HashType
import net.navatwo.kaff4.model.rdf.ImageStream
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.model.rdf.None
import net.navatwo.kaff4.model.rdf.createArn
import net.navatwo.kaff4.rdf.MemoryRdfRepositoryPlugin
import net.navatwo.kaff4.streams.TestAff4ContainerBuilderModule
import net.navatwo.kaff4.streams.compression.Aff4SnappyPlugin
import net.navatwo.kaff4.streams.compression.SnappyCompression
import net.navatwo.kaff4.streams.image_stream.Bevy
import net.navatwo.kaff4.streams.symbolics.Symbolics
import net.navatwo.test.GuiceModule
import net.navatwo.test.OffTestThreadExecutor
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.function.Consumer
import javax.inject.Inject

class Aff4MapStreamSinkTest {

  @GuiceModule
  val module = Modules.combine(
    TestAff4ContainerBuilderModule,
    Aff4BaseStreamModule,
    MemoryRdfRepositoryPlugin,
    Aff4SnappyPlugin,
    TestActionScopeModule,
  )

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var bevyFactory: Bevy.Factory

  @Inject
  private lateinit var symbolics: Symbolics

  @Inject
  private lateinit var sha256FileSystemFactory: Sha256FileSystemFactory

  @Inject
  private lateinit var offTestThreadExecutor: OffTestThreadExecutor

  @UsingTemporary
  private lateinit var tempFileSystem: FileSystem

  @Inject
  private lateinit var snappyCompression: SnappyCompression

  @Inject
  @field:DefaultToolDialect
  private lateinit var toolDialect: ToolDialect

  private val imageFileSystem by lazy { sha256FileSystemFactory.create(tempFileSystem, "sha256".toPath()) }

  private val outputFileSystem by lazy { tempFileSystem.relativeTo("output".toPath()) }

  @Inject
  private lateinit var aff4ImageOpener: Aff4ImageOpener

  @Inject
  private lateinit var aff4ContainerBuilderFactory: Aff4ContainerBuilder.Factory

  private val buffer = Buffer()

  private lateinit var containerArn: Aff4Arn
  private lateinit var aff4ContainerBuilder: RealAff4ContainerBuilder

  @BeforeEach
  fun setup() {
    containerArn = valueFactory.createArn("aff4://ffffffff-308f-4235-838c-e20a8898ad00")
    aff4ContainerBuilder = aff4ContainerBuilderFactory.create(
      Aff4ContainerBuilder.Context(
        temporaryFileSystem = imageFileSystem,
        arn = containerArn,
        toolDialect = toolDialect,
      ),
    ) as RealAff4ContainerBuilder
  }

  @Test
  fun `create map sink and write out of order`() {
    val chunkSize = 5
    val chunksInSegment = 2

    val dataStream = ImageStream(
      arn = valueFactory.createIRI("aff4://cccccccc-649c-494b-923f-e4ed0c5afef4"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = Long.MAX_VALUE,
      compressionMethod = CompressionMethod.None,
      stored = containerArn,
      linearHashes = listOf(HashType.MD5).map { it.value(ByteString.EMPTY) }.toSet(),
    )

    val mapStream = MapStream(
      arn = valueFactory.createIRI("aff4://cccccccc-7598-4283-b183-fc1424edf533"),
      mapGapDefaultStream = symbolics.zero.arn,
      dependentStreams = setOf(dataStream.arn),
      size = Long.MAX_VALUE,
      stored = containerArn,
    )

    val firstContent = "abcdefghijklmno".encodeUtf8()
    val contentOffset = 100L
    val secondContent = "pqrstuvwxyz".encodeUtf8()
    val backWrittenContentOffset = 10

    var expectedSize = 0L
    var expectedPosition = 0L
    var expectedDataSize = 0L

    val (finalMapStream, finalDataStream) = aff4ContainerBuilder.createImageStream(dataStream, listOf())
      .use { dataStreamSink ->
        val finalMapStream = aff4ContainerBuilder.createMapStream(mapStream, dataStreamSink).use { mapStreamSink ->
          // Write and validate that the position/size APIs are behaving. That is, the cursor can be moved arbitrarily,
          // however the size will only grow once content is written into that position.
          buffer.write(firstContent, 0, backWrittenContentOffset)
          mapStreamSink.write(buffer, buffer.size)

          expectedPosition += backWrittenContentOffset
          expectedSize += backWrittenContentOffset
          // buffering so no data written yet

          assertThat(mapStreamSink.position).isEqualTo(expectedPosition)
          assertThat(mapStreamSink.size).isEqualTo(expectedSize)

          assertThat(dataStreamSink.size).isEqualTo(expectedDataSize)

          mapStreamSink.position += contentOffset

          expectedPosition += contentOffset
          expectedDataSize += backWrittenContentOffset // chunks are written, but size is only the original value

          assertThat(mapStreamSink.position).isEqualTo(expectedPosition)
          assertThat(mapStreamSink.size).isEqualTo(expectedSize)

          assertThat(dataStreamSink.size).isEqualTo(expectedDataSize)

          buffer.write(secondContent)
          mapStreamSink.write(buffer, buffer.size)

          expectedPosition += secondContent.size
          // Size is now respected by writing
          expectedSize += contentOffset + secondContent.size

          assertThat(mapStreamSink.position).isEqualTo(expectedPosition)
          assertThat(mapStreamSink.size).isEqualTo(expectedSize)

          assertThat(dataStreamSink.size).isEqualTo(expectedDataSize)

          mapStreamSink.position = backWrittenContentOffset.toLong()

          expectedPosition = backWrittenContentOffset.toLong()
          expectedDataSize += secondContent.size

          assertThat(mapStreamSink.position).isEqualTo(expectedPosition)
          assertThat(mapStreamSink.size).isEqualTo(expectedSize)

          assertThat(dataStreamSink.size).isEqualTo(expectedDataSize)

          buffer.write(firstContent, backWrittenContentOffset, firstContent.size - backWrittenContentOffset)
          mapStreamSink.write(buffer, buffer.size)

          // data is buffered from a small write
          expectedPosition += firstContent.size - backWrittenContentOffset

          assertThat(mapStreamSink.position).isEqualTo(expectedPosition)
          assertThat(mapStreamSink.size).isEqualTo(expectedSize)

          assertThat(dataStreamSink.size).isEqualTo(expectedDataSize)

          mapStreamSink.close()

          expectedDataSize += firstContent.size - backWrittenContentOffset

          assertThat(mapStreamSink.position).isEqualTo(expectedPosition)
          assertThat(mapStreamSink.size).isEqualTo(expectedSize)

          assertThat(dataStreamSink.size).isEqualTo(expectedDataSize)

          mapStreamSink.mapStream
        }

        dataStreamSink.close()

        finalMapStream to dataStreamSink.imageStream
      }

    assertThat(finalMapStream.size).isEqualTo(expectedSize)
    assertThat(finalDataStream.size).isEqualTo(expectedDataSize)

    // Golden value: just the data stream in the index
    assertThat(imageFileSystem).md5(finalMapStream.idxPath, "971209f68aaa555dc6bdd98a550d72c9")

    // Golden value: Map of only 3 entries first[0..9] -> first[10..14] -> second[0..10]. first[..] is split in two
    // because it was written out of order and the image stream doesn't line up. The entries are written in a consistent
    // random order.
    assertThat(imageFileSystem).md5(finalMapStream.mapPath, "9449f8b42685155358dce6bf517b0919")

    // not supported today
    assertThat(imageFileSystem).doesNotExist(finalMapStream.mapPathPath)

    assertThat(finalDataStream.linearHashes)
      .containsExactly(HashType.MD5.value("20c87aeb86f28ce61e041c322f2a626c".decodeHex()))

    // Got 3 bevies, just sanity check them
    assertThat((0 until 3).map { bevyFactory.create(dataStream, it, listOf()) })
      .allSatisfy(
        Consumer { bevy ->
          assertThat(imageFileSystem).exists(bevy.dataSegment)
          assertThat(imageFileSystem).exists(bevy.indexSegment)
        }
      )

    verifyWrittenStream(finalMapStream)
  }

  @Test
  fun `write large symbolic chunks`() {
    val chunkSize = 1024
    val chunksInSegment = 10

    val dataStream = ImageStream(
      arn = valueFactory.createIRI("aff4://aaaaaaaa-649c-494b-923f-e4ed0c5afef4"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = Long.MAX_VALUE,
      compressionMethod = CompressionMethod.None,
      stored = containerArn,
      linearHashes = listOf(HashType.MD5).map { it.value(ByteString.EMPTY) }.toSet(),
    )

    val mapStream = MapStream(
      arn = valueFactory.createIRI("aff4://bbbbbbbb-7598-4283-b183-fc1424edf533"),
      mapGapDefaultStream = symbolics.zero.arn,
      dependentStreams = setOf(dataStream.arn),
      size = Long.MAX_VALUE,
      stored = containerArn,
    )

    var expectedPosition = 0L
    var expectedSize = 0L

    val (finalMapStream, finalDataStream) = aff4ContainerBuilder.createImageStream(dataStream, listOf())
      .use { dataStreamSink ->
        val finalMapStream = aff4ContainerBuilder.createMapStream(mapStream, dataStreamSink).use { writtenMapStream ->
          // Write and validate that the position/size APIs are behaving. That is, the cursor can be moved arbitrarily,
          // however the size will only grow once content is written into that position.
          val ffString = 0xff.repeatByteString(chunkSize * 10)
          val gapString = 0x00.repeatByteString(chunkSize * 5)
          val aaString = 0xaa.repeatByteString(chunkSize * 3)

          buffer.write(ffString)
          writtenMapStream.write(buffer, buffer.size)

          expectedPosition += ffString.size
          expectedSize += ffString.size
          assertThat(writtenMapStream.position).isEqualTo(expectedPosition)
          assertThat(writtenMapStream.size).isEqualTo(expectedSize)

          buffer.write(gapString)
          writtenMapStream.write(buffer, buffer.size)

          expectedPosition += gapString.size
          expectedSize += gapString.size
          assertThat(writtenMapStream.position).isEqualTo(expectedPosition)
          assertThat(writtenMapStream.size).isEqualTo(expectedSize)

          buffer.write(aaString)
          writtenMapStream.write(buffer, buffer.size)

          expectedPosition += aaString.size
          expectedSize += aaString.size
          assertThat(writtenMapStream.position).isEqualTo(expectedPosition)
          assertThat(writtenMapStream.size).isEqualTo(expectedSize)

          writtenMapStream.position += 50

          expectedPosition += 50
          assertThat(writtenMapStream.position).isEqualTo(expectedPosition)
          assertThat(writtenMapStream.size).isEqualTo(expectedSize)

          buffer.write(gapString)
          writtenMapStream.write(buffer, buffer.size)

          expectedPosition += gapString.size
          expectedSize += gapString.size + 50
          assertThat(writtenMapStream.position).isEqualTo(expectedPosition)
          assertThat(writtenMapStream.size).isEqualTo(expectedSize)

          writtenMapStream.close()
          writtenMapStream.mapStream
        }

        dataStreamSink.close()

        finalMapStream to dataStreamSink.imageStream
      }

    assertThat(finalMapStream.size).isEqualTo(expectedSize)
    assertThat(finalDataStream.size).isEqualTo(0L)

    // Golden value: Empty targets file but does exist
    assertThat(imageFileSystem).md5(finalMapStream.idxPath, "4feb912cba2126fbf780da081a6eb349")

    // Golden value: Map of 4 entries 0xff -> 0x00 -> 0xaa -> 0x00. The values are randomly assorted to be:
    // 1. 0x00 @ 18482 for 5  * chunkSize
    // 2. 0xff @ 0     for 10 * chunkSize
    // 3. 0x00 @ 10240 for 5 * chunkSize
    // 4. 0xaa @ 15360 for 3 * chunkSize
    assertThat(imageFileSystem).md5(finalMapStream.mapPath, "d3a525cfeb5f58dbc81461c2697d8bfe")

    // not supported today
    assertThat(imageFileSystem).doesNotExist(finalMapStream.mapPathPath)

    assertThat(finalDataStream.linearHashes)
      .containsExactly(HashType.MD5.value("d41d8cd98f00b204e9800998ecf8427e".decodeHex()))

    verifyWrittenStream(finalMapStream)
  }

  @Test
  fun `create snappy bevy`() {
    val chunkSize = 1024
    val chunksInSegment = 20

    val dataStream = ImageStream(
      arn = valueFactory.createIRI("aff4://aaaaaaaa-649c-494b-923f-e4ed0c5afef4"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = Long.MAX_VALUE,
      compressionMethod = snappyCompression,
      stored = containerArn,
      linearHashes = listOf(HashType.MD5).map { it.value(ByteString.EMPTY) }.toSet(),
    )

    val mapStream = MapStream(
      arn = valueFactory.createIRI("aff4://bbbbbbbb-7598-4283-b183-fc1424edf533"),
      mapGapDefaultStream = symbolics.zero.arn,
      dependentStreams = setOf(dataStream.arn),
      size = Long.MAX_VALUE,
      stored = containerArn,
    )

    var expectedPosition = 0L
    var expectedSize = 0L
    var expectedDataSize = 0L

    val abcdString = "abcd".repeat(chunkSize).encodeUtf8()
    val efghString = "efgh".repeat(chunkSize * 5).encodeUtf8()

    val (finalMapStream, finalDataStream) = aff4ContainerBuilder.createImageStream(dataStream, listOf())
      .use { dataStreamSink ->
        val finalMapStream = aff4ContainerBuilder.createMapStream(mapStream, dataStreamSink).use { writtenMapStream ->

          // Write and validate that the position/size APIs are behaving. That is, the cursor can be moved arbitrarily,
          // however the size will only grow once content is written into that position.
          buffer.write(abcdString)
          writtenMapStream.write(buffer, buffer.size)

          expectedPosition += abcdString.size
          expectedSize += abcdString.size
          // Data hasn't flushed yet due to chunk size
          // expectedDataSize += abcdString.size
          assertThat(writtenMapStream.position).isEqualTo(expectedPosition)
          assertThat(writtenMapStream.size).isEqualTo(expectedSize)
          assertThat(dataStreamSink.size).isEqualTo(expectedDataSize)

          // write twice!
          buffer.write(abcdString)
          writtenMapStream.write(buffer, buffer.size)

          expectedPosition += abcdString.size
          expectedSize += abcdString.size
          // Still hasn't...
          // expectedDataSize += abcdString.size
          assertThat(writtenMapStream.position).isEqualTo(expectedPosition)
          assertThat(writtenMapStream.size).isEqualTo(expectedSize)
          assertThat(dataStreamSink.size).isEqualTo(expectedDataSize)

          buffer.write(efghString)
          writtenMapStream.write(buffer, buffer.size)

          expectedPosition += efghString.size
          expectedSize += efghString.size
          // Now we flushed with bigger writes
          expectedDataSize += 2 * abcdString.size + efghString.size

          assertThat(writtenMapStream.position).isEqualTo(expectedPosition)
          assertThat(writtenMapStream.size).isEqualTo(expectedSize)
          assertThat(dataStreamSink.size).isEqualTo(expectedDataSize)

          writtenMapStream.position += 50

          expectedPosition += 50
          assertThat(writtenMapStream.position).isEqualTo(expectedPosition)
          assertThat(writtenMapStream.size).isEqualTo(expectedSize)
          assertThat(dataStreamSink.size).isEqualTo(expectedDataSize)

          buffer.write(abcdString)
          writtenMapStream.write(buffer, buffer.size)

          expectedPosition += abcdString.size
          expectedSize += abcdString.size + 50
          assertThat(writtenMapStream.position).isEqualTo(expectedPosition)
          assertThat(writtenMapStream.size).isEqualTo(expectedSize)
          assertThat(dataStreamSink.size).isEqualTo(expectedDataSize)

          writtenMapStream.close()

          expectedDataSize += abcdString.size
          assertThat(writtenMapStream.position).isEqualTo(expectedPosition)
          assertThat(writtenMapStream.size).isEqualTo(expectedSize)
          assertThat(dataStreamSink.size).isEqualTo(expectedDataSize)

          writtenMapStream.mapStream
        }

        dataStreamSink.close()

        finalMapStream to dataStreamSink.imageStream
      }

    assertThat(finalMapStream.size).isEqualTo(expectedSize)
    assertThat(finalDataStream.size).isEqualTo(expectedDataSize)

    // Golden value: Single target element
    assertThat(imageFileSystem).md5(finalMapStream.idxPath, "5e5729c273fb3b9059e7a6ad961c1e43")

    // Golden value: Map of 2 entries split by the position += 50 because all content is in the data stream
    assertThat(imageFileSystem).md5(finalMapStream.mapPath, "ab87d754cc3780936c071ac64281052a")

    // not supported today
    assertThat(imageFileSystem).doesNotExist(finalMapStream.mapPathPath)

    verifyWrittenStream(finalMapStream)
  }

  private fun verifyWrittenStream(writtenMapStream: MapStream) {
    aff4ContainerBuilder.buildIntoDirectory(outputFileSystem, ".".toPath())

    offTestThreadExecutor.synchronous {
      aff4ImageOpener.open(outputFileSystem, ".".toPath()) { container ->
        val openedImageStream = container.streamOpener.openStream(writtenMapStream.arn) as Aff4MapStreamSourceProvider
        assertThat(openedImageStream.mapStream).isEqualTo(writtenMapStream)

        openedImageStream.verify(container.aff4Model)
      }
    }
  }
}
