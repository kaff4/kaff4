package com.github.nava2.aff4.io

import okio.Buffer
import okio.ByteString
import okio.Source
import okio.Timeout

internal fun ByteString.source(timeout: Timeout = Timeout.NONE): Source {
  return object : Source {
    private val buffer = asByteBuffer()

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
  }
}

internal fun Source.fixedLength(length: Long): Source {
  return object : Source by this@fixedLength {
    private val delegate = this@fixedLength

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
  }
}
