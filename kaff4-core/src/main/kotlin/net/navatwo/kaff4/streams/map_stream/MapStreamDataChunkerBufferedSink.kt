package net.navatwo.kaff4.streams.map_stream

import io.github.zabuzard.fastcdc4j.external.chunking.Chunk
import io.github.zabuzard.fastcdc4j.external.chunking.Chunker
import io.github.zabuzard.fastcdc4j.external.chunking.ChunkerBuilder
import net.navatwo.kaff4.streams.symbolics.Symbolics
import okio.Buffer
import okio.Sink
import okio.Timeout
import okio.use
import java.nio.ByteBuffer
import kotlin.math.roundToLong

private const val MINIMUM_CHUNKS_FOR_CHUNK_NORMALIZATION_REDUCTION = 10
private const val DEFAULT_MINIMUM_CHUNK_SIZE_FACTOR = 0.25
private const val MAXIMAL_CHUNK_SIZE_FACTOR = 1.0

private const val DEFAULT_CHUNKS_TO_BUFFER = 10
private const val MINIMUM_SYMBOLIC_CHUNK_SIZE = 3L

internal class MapStreamDataChunkerBufferedSink(
  private val symbolics: Symbolics,
  private val timeout: Timeout,
  private val chunkSize: Int,
  private val mapDataChunkSink: MapDataChunkSink,
  chunksToBuffer: Int = DEFAULT_CHUNKS_TO_BUFFER,
  minimumChunkSizeFactor: Double = DEFAULT_MINIMUM_CHUNK_SIZE_FACTOR,
) : Sink {
  private val bufferLimit = chunkSize * chunksToBuffer
  private val symbolicChunkMinimumSize = (chunkSize * minimumChunkSizeFactor).roundToLong()
    .coerceAtLeast(MINIMUM_SYMBOLIC_CHUNK_SIZE)

  private val buffer = Buffer()

  private val chunker: Chunker = ChunkerBuilder()
    .fastCdc()
    .setMinimalChunkSizeFactor(minimumChunkSizeFactor)
    .setExpectedChunkSize(chunkSize)
    .setMaximalChunkSizeFactor(MAXIMAL_CHUNK_SIZE_FACTOR)
    .apply {
      if (chunkSize <= MINIMUM_CHUNKS_FOR_CHUNK_NORMALIZATION_REDUCTION) {
        setNormalizationLevel(1)
      }
    }
    .build()

  @Volatile
  private var closed = false

  override fun close() {
    if (closed) return
    synchronized(this) {
      if (closed) return
      closed = true
    }

    flushUnsafe()

    buffer.clear()
    buffer.close()

    mapDataChunkSink.close()
  }

  override fun write(source: Buffer, byteCount: Long) {
    check(!closed) { "closed" }
    buffer.write(source, byteCount)

    for (chunk in chunkSequence(readAllBufferedChunks = false)) {
      mapDataChunkSink.write(chunk)
    }
  }

  override fun timeout(): Timeout = timeout

  override fun flush() {
    check(!closed) { "closed" }

    flushUnsafe()
  }

  private fun flushUnsafe() {
    timeout.throwIfReached()

    buffer.flush()

    for (chunk in chunkSequence(readAllBufferedChunks = true)) {
      mapDataChunkSink.write(chunk)
    }

    mapDataChunkSink.flush()
  }

  private fun chunkSequence(readAllBufferedChunks: Boolean): Sequence<MapDataChunk> {
    if (buffer.size == 0L) return sequenceOf()

    // We buffer chunks, so avoid writing them until the buffer size is large enough to have good performance unless
    // forced to do so (closing a map, moving the cursor, etc.)
    if (!readAllBufferedChunks && buffer.size < bufferLimit) {
      return sequenceOf()
    }

    // We need to use a suspending sequence because we need to keep the result in scope of the `bufferInput`, otherwise
    // we could use a direct `map` and return a `Collection<>` but there can potentially be a lot of data being copied
    // and this avoids the excess.
    return sequence {
      buffer.inputStream().use { bufferInput ->
        for (chunk in chunker.chunk(bufferInput, buffer.size)) {
          check(chunk.length > 0) {
            "wat"
          }
          timeout.throwIfReached()

          val mapChunk = computeChunkFromBuffer(chunk.wrapAsBuffer())
          yield(mapChunk)
        }
      }
    }
  }

  private fun computeChunkFromBuffer(chunkBuffer: ByteBuffer): MapDataChunk {
    return maybeGetSymbolicIriFromChunk(chunkBuffer)
      ?: MapDataChunk.Data(chunkBuffer)
  }

  private fun maybeGetSymbolicIriFromChunk(chunkBuffer: ByteBuffer): MapDataChunk.Symbolic? {
    if (chunkBuffer.limit() < symbolicChunkMinimumSize) return null

    chunkBuffer.mark()

    val firstByte = chunkBuffer.get()

    try {
      while (chunkBuffer.hasRemaining() && firstByte == chunkBuffer.get()) {
        // exhaust if possible
      }

      // if true, we didn't completely iterate it and the chunk isn't uniform
      if (chunkBuffer.hasRemaining()) return null
    } finally {
      chunkBuffer.reset()
    }

    val symbolicArn = symbolics.getArnForSimplePattern(firstByte)
    return MapDataChunk.Symbolic(symbolicArn, chunkBuffer.remaining())
  }
}

private fun Chunk.wrapAsBuffer() = ByteBuffer.wrap(data, 0, length).asReadOnlyBuffer()
