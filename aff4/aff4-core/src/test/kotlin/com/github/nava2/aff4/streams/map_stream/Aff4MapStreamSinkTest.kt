package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.io.Sha256FileSystemFactory
import com.github.nava2.aff4.io.doesNotExist
import com.github.nava2.aff4.io.exists
import com.github.nava2.aff4.io.md5
import com.github.nava2.aff4.io.repeatByteString
import com.github.nava2.aff4.model.rdf.CompressionMethod
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryModule
import com.github.nava2.aff4.streams.WritingModule
import com.github.nava2.aff4.streams.compression.SnappyCompression
import com.github.nava2.aff4.streams.image_stream.Aff4ImageStreamSink
import com.github.nava2.aff4.streams.image_stream.Bevy
import com.github.nava2.aff4.streams.symbolics.Symbolics
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
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Random
import java.util.function.Consumer
import javax.inject.Inject

class Aff4MapStreamSinkTest {
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
  private lateinit var symbolics: Symbolics

  private val consistentRandom: Random = Random(0L)

  @Inject
  private lateinit var sha256FileSystemFactory: Sha256FileSystemFactory

  @Inject
  private lateinit var snappyCompression: SnappyCompression

  private val imageFileSystem: FileSystem by lazy { sha256FileSystemFactory.create(tempDirectory) }

  private val containerArn by lazy { valueFactory.createIRI("aff4://99cc4380-308f-4235-838c-e20a8898ad00") }

  @Test
  fun `create map sink and write out of order`() {
    val chunkSize = 5
    val chunksInSegment = 2

    val dataStream = ImageStream(
      arn = valueFactory.createIRI("aff4://bb362b22-649c-494b-923f-e4ed0c5afef4"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = Long.MAX_VALUE,
      compressionMethod = CompressionMethod.None,
      stored = containerArn,
      linearHashes = listOf(HashType.MD5).map { it.value(ByteString.EMPTY) },
    )

    val mapStream = MapStream(
      arn = valueFactory.createIRI("aff4://f5ce6c33-7598-4283-b183-fc1424edf533"),
      mapGapDefaultStream = symbolics.zero.arn,
      dependentStream = dataStream.arn,
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

    val (finalMapStream, finalDataStream) = Aff4ImageStreamSink(
      bevyFactory = bevyFactory,
      outputFileSystem = imageFileSystem,
      imageStream = dataStream,
      blockHashTypes = listOf(),
      timeout = Timeout.NONE,
    ).use { dataStreamSink ->
      val finalMapStream = Aff4MapStreamSink(
        random = consistentRandom,
        symbolics = symbolics,
        outputFileSystem = imageFileSystem,
        dataStreamSink = dataStreamSink,
        mapStream = mapStream,
        timeout = Timeout.NONE,
      ).use { mapStreamSink ->
        // Write and validate that the position/size APIs are behaving. That is, the cursor can be moved arbitrarily,
        // however the size will only grow once content is written into that position.
        Buffer().use { buffer ->
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
        }

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
    assertThat(imageFileSystem).md5(finalMapStream.idxPath(containerArn), "f0976c8f0e9137a455ab7c92091622c7")

    // Golden value: Map of only 3 entries first[0..9] -> first[10..14] -> second[0..10]. first[..] is split in two
    // because it was written out of order and the image stream doesn't line up. The entries are written in a consistent
    // random order.
    assertThat(imageFileSystem).md5(finalMapStream.mapPath(containerArn), "9449f8b42685155358dce6bf517b0919")

    // not supported today
    assertThat(imageFileSystem).doesNotExist(finalMapStream.mapPathPath(containerArn))

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
  }

  @Test
  fun `write large symbolic chunks`() {
    val chunkSize = 1024
    val chunksInSegment = 10

    val dataStream = ImageStream(
      arn = valueFactory.createIRI("aff4://bb362b22-649c-494b-923f-e4ed0c5afef4"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = Long.MAX_VALUE,
      compressionMethod = CompressionMethod.None,
      stored = containerArn,
      linearHashes = listOf(HashType.MD5).map { it.value(ByteString.EMPTY) },
    )

    val mapStream = MapStream(
      arn = valueFactory.createIRI("aff4://f5ce6c33-7598-4283-b183-fc1424edf533"),
      mapGapDefaultStream = symbolics.zero.arn,
      dependentStream = dataStream.arn,
      size = Long.MAX_VALUE,
      stored = containerArn,
    )

    var expectedPosition = 0L
    var expectedSize = 0L

    val (finalMapStream, finalDataStream) = Aff4ImageStreamSink(
      bevyFactory = bevyFactory,
      outputFileSystem = imageFileSystem,
      imageStream = dataStream,
      blockHashTypes = listOf(),
      timeout = Timeout.NONE,
    ).use { dataStreamSink ->
      val finalMapStream = Aff4MapStreamSink(
        random = consistentRandom,
        symbolics = symbolics,
        outputFileSystem = imageFileSystem,
        dataStreamSink = dataStreamSink,
        mapStream = mapStream,
        timeout = Timeout.NONE,
      ).use { writtenMapStream ->
        // Write and validate that the position/size APIs are behaving. That is, the cursor can be moved arbitrarily,
        // however the size will only grow once content is written into that position.
        Buffer().use { buffer ->
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
        }

        writtenMapStream.close()
        writtenMapStream.mapStream
      }

      dataStreamSink.close()

      finalMapStream to dataStreamSink.imageStream
    }

    assertThat(finalMapStream.size).isEqualTo(expectedSize)
    assertThat(finalDataStream.size).isEqualTo(0L)

    // Golden value: Empty targets file but does exist
    assertThat(imageFileSystem).md5(finalMapStream.idxPath(containerArn), "4feb912cba2126fbf780da081a6eb349")

    // Golden value: Map of 4 entries 0xff -> 0x00 -> 0xaa -> 0x00. The values are randomly assorted to be:
    // 1. 0x00 @ 18482 for 5  * chunkSize
    // 2. 0xff @ 0     for 10 * chunkSize
    // 3. 0x00 @ 10240 for 5 * chunkSize
    // 4. 0xaa @ 15360 for 3 * chunkSize
    assertThat(imageFileSystem).md5(finalMapStream.mapPath(containerArn), "d3a525cfeb5f58dbc81461c2697d8bfe")

    // not supported today
    assertThat(imageFileSystem).doesNotExist(finalMapStream.mapPathPath(containerArn))

    assertThat(finalDataStream.linearHashes)
      .containsExactly(HashType.MD5.value("d41d8cd98f00b204e9800998ecf8427e".decodeHex()))
  }

  @Test
  fun `create snappy bevy`() {
    val chunkSize = 1024
    val chunksInSegment = 20

    val dataStream = ImageStream(
      arn = valueFactory.createIRI("aff4://bb362b22-649c-494b-923f-e4ed0c5afef4"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = Long.MAX_VALUE,
      compressionMethod = snappyCompression,
      stored = containerArn,
      linearHashes = listOf(HashType.MD5).map { it.value(ByteString.EMPTY) },
    )

    val mapStream = MapStream(
      arn = valueFactory.createIRI("aff4://f5ce6c33-7598-4283-b183-fc1424edf533"),
      mapGapDefaultStream = symbolics.zero.arn,
      dependentStream = dataStream.arn,
      size = Long.MAX_VALUE,
      stored = containerArn,
    )

    var expectedPosition = 0L
    var expectedSize = 0L
    var expectedDataSize = 0L

    val abcdString = "abcd".repeat(chunkSize).encodeUtf8()
    val efghString = "efgh".repeat(chunkSize * 5).encodeUtf8()

    val (finalMapStream, finalDataStream) = Aff4ImageStreamSink(
      bevyFactory = bevyFactory,
      outputFileSystem = imageFileSystem,
      imageStream = dataStream,
      blockHashTypes = listOf(),
      timeout = Timeout.NONE,
    ).use { dataStreamSink ->
      val finalMapStream = Aff4MapStreamSink(
        random = consistentRandom,
        symbolics = symbolics,
        outputFileSystem = imageFileSystem,
        dataStreamSink = dataStreamSink,
        mapStream = mapStream,
        timeout = Timeout.NONE,
      ).use { writtenMapStream ->

        // Write and validate that the position/size APIs are behaving. That is, the cursor can be moved arbitrarily,
        // however the size will only grow once content is written into that position.
        Buffer().use { buffer ->
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
        }

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
    assertThat(imageFileSystem).md5(finalMapStream.idxPath(containerArn), "f0976c8f0e9137a455ab7c92091622c7")

    // Golden value: Map of 2 entries split by the position += 50 because all content is in the data stream
    assertThat(imageFileSystem).md5(finalMapStream.mapPath(containerArn), "ab87d754cc3780936c071ac64281052a")

    // not supported today
    assertThat(imageFileSystem).doesNotExist(finalMapStream.mapPathPath(containerArn))
  }
}
