package com.github.nava2.aff4.io

import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import okio.Source
import okio.Timeout

fun BufferedSource.lineSequence(): Sequence<String> = sequence {
  var targetLine = readUtf8Line()
  while (targetLine != null) {
    yield(targetLine)
    targetLine = readUtf8Line()
  }
}

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

  override fun timeout(): Timeout = timeout
  override fun close() = delegate.close()

  override fun toString(): String = "byteString($byteString)"
}
