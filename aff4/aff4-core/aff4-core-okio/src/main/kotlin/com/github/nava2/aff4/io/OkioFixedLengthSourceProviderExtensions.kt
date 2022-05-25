package com.github.nava2.aff4.io

import okio.Buffer
import okio.Source
import okio.Timeout

fun <SOURCE : Source> SourceProvider<SOURCE>.bounded(position: Long, length: Long): SourceProvider<Source> {
  require(position >= 0L) { "position < 0" }
  require(length >= 0L) { "length < 0" }

  // offset first, then limit as the limit is after it's moved to offset
  return offset(position).limit(length)
}

fun <SOURCE : Source> SourceProvider<SOURCE>.offset(position: Long): SourceProvider<Source> {
  require(position >= 0L)
  if (position == 0L) return this

  return buffer().transform { source ->
    source.skip(position)
    source
  }
}

fun <SOURCE : Source> SourceProvider<SOURCE>.limit(length: Long): SourceProvider<Source> {
  return if (length == 0L) {
    NullSourceProvider
  } else {
    transform { source -> FixedLengthSource(source, length) }
  }
}

private class FixedLengthSource(
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
