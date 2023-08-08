package net.navatwo.kaff4.io

import okio.Buffer
import okio.ByteString
import okio.Timeout

fun ByteString.source(timeout: Timeout = Timeout.NONE): Source {
  return ByteStringSource(this, timeout)
}

private class ByteStringSource(
  private val byteString: ByteString,
  private val timeout: Timeout,
) : Source {
  private val delegate = byteString.asByteBuffer().source(timeout)

  override fun read(sink: Buffer, byteCount: Long): Long {
    return delegate.read(sink, byteCount)
  }

  override fun skip(byteCount: Long): Long {
    return delegate.skip(byteCount)
  }

  override fun timeout(): Timeout = timeout
  override fun close() = delegate.close()

  override fun toString(): String = "byteString($byteString)"
}
