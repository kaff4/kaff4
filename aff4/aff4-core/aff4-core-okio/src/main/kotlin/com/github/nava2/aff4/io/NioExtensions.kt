package com.github.nava2.aff4.io

import okio.Source
import okio.Timeout
import java.nio.Buffer
import java.nio.ByteBuffer

fun Buffer.availableRange(mappedPosition: Long): LongRange {
  val start = mappedPosition - position()
  val last = mappedPosition + remaining()
  return start until last
}

fun ByteBuffer.source(timeout: Timeout = Timeout.NONE): Source {
  return ByteBufferSource(this.asReadOnlyBuffer(), timeout)
}

private class ByteBufferSource(
  private val buffer: ByteBuffer,
  private val timeout: Timeout,
) : Source {
  override fun read(sink: okio.Buffer, byteCount: Long): Long {
    timeout.throwIfReached()

    if (!buffer.hasRemaining()) return -1

    val maxAvailableBytes = byteCount.toInt().coerceAtMost(buffer.remaining())
    val slice = buffer.slice(buffer.position(), maxAvailableBytes)
    val bytesRead = sink.write(slice)

    buffer.position(buffer.position() + bytesRead)

    return bytesRead.toLong()
  }

  override fun timeout(): Timeout = timeout
  override fun close() = Unit

  override fun toString(): String = "byteBuffer($buffer)"
}
