package net.navatwo.kaff4.io

import okio.Buffer
import okio.ByteString
import okio.Timeout

fun ByteString.source(timeout: Timeout = Timeout.NONE): Source {
  return ByteStringSource(this, timeout)
}

private class ByteStringSource(
  private val byteString: ByteString,
  timeout: Timeout,
) : AbstractSource(timeout) {
  private val delegate = byteString.asByteBuffer().source(timeout)

  override fun exhausted(): Source.Exhausted = delegate.exhausted()

  override fun protectedRead(sink: Buffer, byteCount: Long): Long = delegate.read(sink, byteCount)

  override fun protectedClose() = delegate.close()

  override fun toString(): String = "byteString($byteString)"
}
