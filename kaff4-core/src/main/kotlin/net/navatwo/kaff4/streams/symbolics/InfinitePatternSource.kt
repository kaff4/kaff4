package net.navatwo.kaff4.streams.symbolics

import okio.Buffer
import okio.ByteString
import okio.Source
import okio.Timeout
import java.nio.ByteBuffer

internal class InfinitePatternSource(
  private val pattern: ByteString,
  patternBuffer: ByteBuffer,
  private val repetitionBoundary: Int,
  private val timeout: Timeout,
) : Source {
  private var position: Long = 0L

  private val byteBuffer = patternBuffer.asReadOnlyBuffer()

  override fun read(sink: Buffer, byteCount: Long): Long {
    timeout.throwIfReached()
    refreshBufferIfNeeded()

    val maxBytesToRead = byteCount.toInt().coerceAtMost(byteBuffer.remaining())
    val slice = byteBuffer.slice(byteBuffer.position(), maxBytesToRead)

    val bytesRead = sink.write(slice)
    byteBuffer.position(byteBuffer.position() + bytesRead)

    position += bytesRead

    return bytesRead.toLong()
  }

  private fun refreshBufferIfNeeded() {
    if (!byteBuffer.hasRemaining()) {
      byteBuffer.rewind()

      val remainingInChunk = repetitionBoundary - (position % repetitionBoundary).toInt()
      byteBuffer.limit(remainingInChunk.coerceAtMost(byteBuffer.capacity()))
    }
  }

  override fun close() = Unit
  override fun timeout(): Timeout = timeout

  override fun toString(): String = "infinite($pattern)"
}
