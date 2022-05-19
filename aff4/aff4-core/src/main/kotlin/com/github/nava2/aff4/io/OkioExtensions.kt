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

internal fun ByteString.source(timeout: Timeout = Timeout.NONE): Source {
  return ByteStringSource(this, timeout)
}

private class ByteStringSource(
  private val byteString: ByteString,
  private val timeout: Timeout,
) : Source {
  private val buffer = byteString.asByteBuffer()

  override fun read(sink: Buffer, byteCount: Long): Long {
    if (!buffer.hasRemaining()) return -1

    val maxAvailableBytes = byteCount.toInt().coerceAtMost(buffer.remaining())
    val slice = buffer.slice(buffer.position(), maxAvailableBytes)
    val bytesRead = sink.write(slice)

    buffer.position(buffer.position() + bytesRead)

    return bytesRead.toLong()
  }

  override fun timeout(): Timeout = timeout
  override fun close() = Unit

  override fun toString(): String = "byteString($byteString)"
}

internal fun Source.fixedLength(length: Long): Source {
  return FixedLength(this, length)
}

private class FixedLength(
  private val delegate: Source,
  private val length: Long,
) : Source {
  private var position = 0L

  override fun read(sink: Buffer, byteCount: Long): Long {
    if (position == length) return -1L

    val remainingBytes = byteCount.coerceAtMost(length - position)
    val readBytes = delegate.read(sink, remainingBytes)

    return if (readBytes == -1L) {
      -1L
    } else {
      position += readBytes
      readBytes
    }
  }

  override fun close() = delegate.close()

  override fun timeout(): Timeout = delegate.timeout()

  override fun toString(): String = "fixedLength($delegate, $length)"
}
