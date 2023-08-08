package net.navatwo.kaff4.streams.symbolics

import net.navatwo.kaff4.io.AbstractSource
import okio.Buffer
import okio.ByteString
import okio.Timeout
import java.nio.ByteBuffer

internal class InfinitePatternSource(
  private val pattern: ByteString,
  patternBuffer: ByteBuffer,
  private val repetitionBoundary: Int,
  timeout: Timeout,
) : AbstractSource(timeout) {
  private var position: Long = 0L

  private val byteBuffer = patternBuffer.asReadOnlyBuffer()

  override fun protectedRead(sink: Buffer, byteCount: Long): Long {
    refreshBufferIfNeeded()

    val maxBytesToRead = byteCount.toInt().coerceAtMost(byteBuffer.remaining())
    val slice = byteBuffer.slice(byteBuffer.position(), maxBytesToRead)

    val bytesRead = sink.write(slice)
    byteBuffer.position(byteBuffer.position() + bytesRead)

    position += bytesRead

    return bytesRead.toLong()
  }

  // infinite sources always have values
  override fun exhausted(): Exhausted = Exhausted.HAS_VALUES

  override fun protectedClose() = Unit

  private fun refreshBufferIfNeeded() {
    if (byteBuffer.hasRemaining()) return

    byteBuffer.rewind()

    val remainingInChunk = repetitionBoundary - (position % repetitionBoundary).toInt()
    byteBuffer.limit(remainingInChunk.coerceAtMost(byteBuffer.capacity()))
  }

  override fun toString(): String = "infinite($pattern)"
}
