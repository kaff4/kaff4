package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.io.repeatByteString
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryPlugin
import com.github.nava2.aff4.rdf.RdfRepositoryModule
import com.github.nava2.aff4.streams.symbolics.Symbolics
import com.github.nava2.test.GuiceModule
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.Timeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import javax.inject.Inject

class MapStreamDataChunkerBufferedSinkTest {
  @GuiceModule
  val modules = listOf(
    RdfRepositoryModule,
    MemoryRdfRepositoryPlugin,
  )

  @Inject
  private lateinit var symbolics: Symbolics

  private val buffer: Buffer = Buffer()

  @AfterEach
  fun tearDown() {
    buffer.close()
  }

  @Test
  fun `write different chunks into buffer, expect written in buffered manner`() {
    val chunkSink = AccumulatingChunkSink()
    val chunkSize = 1024

    val allChunks = mutableListOf<MapDataChunk>()

    MapStreamDataChunkerBufferedSink(
      symbolics = symbolics,
      timeout = Timeout.NONE,
      chunkSize = chunkSize,
      mapDataChunkSink = chunkSink,
      chunksToBuffer = 4,
    ).use { chunkerSink ->
      val aString = 0xa.repeatByteString(chunkSize * 5)
      val bString = 0xb.repeatByteString(chunkSize * 2)
      val xyzString = "xyz".repeat(chunkSize * 1).encodeUtf8()

      buffer.write(aString)
      chunkerSink.write(buffer, buffer.size - chunkSize / 2)

      chunkSink.verify {
        val chunks = listOf(
          MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xa), chunkSize),
          MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xa), chunkSize),
          MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xa), chunkSize),
          MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xa), chunkSize),
          MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xa), chunkSize / 2),
        )

        allChunks += chunks
        chunks
      }

      buffer.write(bString)
      chunkerSink.write(buffer, buffer.size)

      chunkSink.verify {
        // Requires _at least_ 4 chunks to write to sink, so the sink sees nothing right now
        listOf()
      }

      // write another 3!
      buffer.write(bString)
      chunkerSink.write(buffer, buffer.size)

      chunkSink.verify {
        val expectedDataChunk = Buffer().use { dataBuffer ->
          dataBuffer.write(0xa.repeatByteString(512))
          dataBuffer.write(0xb.repeatByteString(512))
          dataBuffer.readByteString()
        }

        val chunks = listOf(
          MapDataChunk.Data(expectedDataChunk.asByteBuffer()),
          MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xb), chunkSize),
          MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xb), chunkSize),
          MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xb), chunkSize),
          MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xb), 512),
        )

        allChunks += chunks
        chunks
      }

      // write two data chunks,
      buffer.write(xyzString)
      buffer.write(xyzString)
      chunkerSink.write(buffer, buffer.size)

      chunkSink.verify {
        val expectedDataChunks = Buffer().use { dataBuffer ->
          dataBuffer.write(xyzString)
          dataBuffer.write(xyzString)

          (0 until 6).map { dataBuffer.readByteString(1024) }
        }

        val chunks = expectedDataChunks.map { MapDataChunk.Data(it.asByteBuffer()) }
        allChunks += chunks
        chunks
      }
    }

    // all chunks are written on close
    assertThat(chunkSink.pendingChunks).isEmpty()
    assertThat(chunkSink.flushedChunks).isEqualTo(allChunks)
  }

  @Test
  fun `flush immediately dumps entire buffer`() {
    val chunkSink = AccumulatingChunkSink()
    val chunkSize = 1024

    val allChunks = mutableListOf<MapDataChunk>()

    MapStreamDataChunkerBufferedSink(
      symbolics = symbolics,
      timeout = Timeout.NONE,
      chunkSize = chunkSize,
      mapDataChunkSink = chunkSink,
      chunksToBuffer = 4,
    ).use { chunkerSink ->
      val bString = 0xb.repeatByteString(chunkSize * 3)

      buffer.write(bString)
      chunkerSink.write(buffer, buffer.size)

      // Requires _at least_ 4 chunks to write to sink, so the sink sees nothing right now
      chunkSink.verify { listOf() }

      // flush buffer dumps immediately
      chunkerSink.flush()

      chunkSink.verify {
        val chunksToWrite = listOf(
          MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xb), chunkSize),
          MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xb), chunkSize),
          MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xb), chunkSize),
        )

        allChunks += chunksToWrite
        chunksToWrite
      }

      // write another 3!
      buffer.write(bString)
      chunkerSink.write(buffer, buffer.size)

      // Requires _at least_ 4 chunks to write to sink, so the sink sees nothing right now
      chunkSink.verify { listOf() }

      // chunks flushed
      allChunks += listOf(
        MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xb), chunkSize),
        MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xb), chunkSize),
        MapDataChunk.Symbolic(symbolics.getArnForSimplePattern(0xb), chunkSize),
      )
    }

    // all chunks are written on close
    assertThat(chunkSink.pendingChunks).isEmpty()
    assertThat(chunkSink.flushedChunks).containsExactlyElementsOf(allChunks)
  }
}

private class AccumulatingChunkSink : MapDataChunkSink {
  val writtenChunks = mutableListOf<MapDataChunk>()

  val pendingChunks = mutableListOf<MapDataChunk>()

  val flushedChunks = mutableListOf<MapDataChunk>()

  var closed = false
    private set

  override fun write(chunk: MapDataChunk) {
    writtenChunks += chunk
    pendingChunks += chunk
  }

  override fun close() {
    closed = true
    flush()
  }

  override fun flush() {
    flushedChunks += pendingChunks
    pendingChunks.clear()
  }

  override fun timeout(): Timeout = Timeout.NONE

  fun verify(block: () -> List<MapDataChunk>) {
    val writtenChunks = block()

    assertThat(this.writtenChunks).`as` { "writtenChunks" }.containsExactlyElementsOf(writtenChunks)
    this.writtenChunks.clear()
  }
}
