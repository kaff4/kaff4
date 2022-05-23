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

fun Source.fixedLength(length: Long): Source {
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

fun concatLazily(sources: List<() -> Source>, timeout: Timeout = Timeout.NONE): Source {
  return LazyConcatSource(sources, timeout)
}

private class LazyConcatSource(
  private val lazySources: List<() -> Source>,
  private val timeout: Timeout,
) : Source {
  private val iter = lazySources.iterator()
  private var current: Source = iter.next().invoke()

  override fun read(sink: Buffer, byteCount: Long): Long {
    timeout.throwIfReached()

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

  override fun toString(): String = "lazyConcatSource(Sources(${lazySources.size}))"
}
