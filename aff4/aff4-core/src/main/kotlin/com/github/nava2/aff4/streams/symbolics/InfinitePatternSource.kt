package com.github.nava2.aff4.streams.symbolics

import okio.Buffer
import okio.Source
import okio.Timeout
import java.nio.ByteBuffer

internal class InfinitePatternSource(
  patternBuffer: ByteBuffer,
  private val repetitionBoundary: Int,
) : Source {
  private var position: Long = 0L

  private val byteBuffer = patternBuffer.asReadOnlyBuffer()

  override fun read(sink: Buffer, byteCount: Long): Long {
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
  override fun timeout(): Timeout = Timeout.NONE
}
