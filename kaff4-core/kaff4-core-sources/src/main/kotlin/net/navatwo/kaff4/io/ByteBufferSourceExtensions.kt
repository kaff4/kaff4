package net.navatwo.kaff4.io

import okio.Buffer
import okio.Source
import okio.Timeout
import java.nio.ByteBuffer

fun ByteBuffer.source(timeout: Timeout = Timeout.NONE): Source {
  return ByteBufferSource(this.asReadOnlyBuffer(), timeout)
}

private class ByteBufferSource(
  private val buffer: ByteBuffer,
  timeout: Timeout,
) : AbstractSource(timeout) {

  override fun protectedRead(sink: Buffer, byteCount: Long): Long {
    val maxAvailableBytes = byteCount.toInt().coerceAtMost(buffer.remaining())
    val slice = buffer.slice(buffer.position(), maxAvailableBytes)
    val bytesRead = sink.write(slice)

    buffer.position(buffer.position() + bytesRead)

    return bytesRead.toLong()
  }

  override fun exhausted(): Exhausted = Exhausted.from(!buffer.hasRemaining())

  override fun protectedClose() = Unit

  override fun toString(): String = "byteBuffer($buffer)"
}
