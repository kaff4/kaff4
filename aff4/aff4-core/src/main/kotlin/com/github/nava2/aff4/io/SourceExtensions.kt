package com.github.nava2.aff4.io

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.Source
import okio.Timeout

internal fun BufferedSource.exhaust(sink: BufferedSink, byteCount: Long): Long {
  var firstRead = true
  var bytesRemaining = byteCount
  do {
    val bytesRead = read(sink.buffer, bytesRemaining)
    if (bytesRead == -1L) {
      // On first read, we return -1 to follow the source pattern
      return if (firstRead) -1 else byteCount - bytesRemaining
    }

    bytesRemaining -= bytesRead
    firstRead = false
  } while (bytesRemaining > 0 && bytesRead > 0)

  return byteCount - bytesRemaining
}

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

internal fun concatLazily(sources: List<() -> Source>, timeout: Timeout = Timeout.NONE): Source {
  return LazyConcatSource(sources, timeout)
}

private class LazyConcatSource(
  lazySources: List<() -> Source>,
  private val timeout: Timeout,
) : Source {
  private val iter = lazySources.iterator()
  private var current: Source = iter.next().invoke()

  override fun read(sink: Buffer, byteCount: Long): Long {
    val bytesRead = current.read(sink, byteCount)

    return when {
      bytesRead == -1L && !iter.hasNext() -> -1L
      bytesRead == -1L && iter.hasNext() -> {
        current.close()
        current = iter.next().invoke()

        read(sink, byteCount)
      }
      else -> bytesRead
    }
  }

  override fun close() = current.close()
  override fun timeout(): Timeout = timeout
}

internal fun Source.concat(vararg sources: Source, timeout: Timeout = timeout()): Source {
  return ConcatSource(listOf(this, *sources), timeout)
}

private class ConcatSource(
  sources: List<Source>,
  private val timeout: Timeout,
) : Source {
  private val iter = sources.iterator()
  private var current: Source = iter.next()

  override fun read(sink: Buffer, byteCount: Long): Long {
    val bytesRead = current.read(sink, byteCount)

    return when {
      bytesRead == -1L && !iter.hasNext() -> -1L
      bytesRead == -1L && iter.hasNext() -> {
        current = iter.next()

        read(sink, byteCount)
      }
      else -> bytesRead
    }
  }

  override fun close() = current.close()
  override fun timeout(): Timeout = timeout
}
