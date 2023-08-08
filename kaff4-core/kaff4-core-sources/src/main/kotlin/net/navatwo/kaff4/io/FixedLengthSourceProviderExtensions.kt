package net.navatwo.kaff4.io

import okio.Buffer

fun <SOURCE : Source> SourceProvider<SOURCE>.bounded(position: Long, length: Long): SourceProvider<Source> {
  require(position >= 0L) { "position < 0" }
  require(length >= 0L) { "length < 0" }

  // offset first, then limit as the limit is after it's moved to offset
  return offset(position).limit(length)
}

fun <SOURCE : Source> SourceProvider<SOURCE>.offset(position: Long): SourceProvider<Source> {
  require(position >= 0L)
  if (position == 0L) return this

  return transform { source ->
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
  override val wrapped: Source,
  private val length: Long,
) : AbstractSource(wrapped.timeout()), WrappingSource {
  private var position = 0L

  override fun protectedRead(sink: Buffer, byteCount: Long): Long {
    val remainingBytes = byteCount.coerceAtMost(length - position)
    val readBytes = wrapped.read(sink, remainingBytes)

    return if (readBytes == -1L) {
      -1L
    } else {
      position += readBytes
      readBytes
    }
  }

  override fun protectedSkip(byteCount: Long): Long {
    val remainingBytes = byteCount.coerceAtMost(length - position)
    val skippedBytes = wrapped.skip(remainingBytes)
    position += skippedBytes
    return skippedBytes
  }

  override fun exhausted(): Exhausted = Exhausted.from(position == length)

  override fun protectedClose() = wrapped.close()

  override fun toString(): String = "fixedLength($wrapped, $length)"
}
