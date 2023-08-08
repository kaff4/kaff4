package net.navatwo.kaff4.io

import net.navatwo.kaff4.io.Source.Exhausted
import net.navatwo.kaff4.io.Source.Exhausted.Companion.positionExhausted
import okio.Buffer

fun <SOURCE : Source> SourceProvider<SOURCE>.bounded(position: Long, size: Long): SourceProvider<Source> {
  require(position >= 0L) { "position < 0" }
  require(size >= 0L) { "size < 0" }

  // offset first, then limit as the limit is after it's moved to offset
  return offset(position).limit(size)
}

fun <SOURCE : Source> SourceProvider<SOURCE>.offset(position: Long): SourceProvider<Source> {
  require(position >= 0L)
  if (position == 0L) return this

  return buffer().transform { source ->
    source.skipFully(position)
    source
  }
}

fun <SOURCE : Source> SourceProvider<SOURCE>.limit(size: Long): SourceProvider<Source> {
  return if (size == 0L) {
    NullSourceProvider
  } else {
    transform { source -> FixedSizeSource(source, size) }
  }
}

private class FixedSizeSource(
  override val wrapped: Source,
  override val size: Long,
) : AbstractSource(wrapped.timeout()), WrappingSource, Sized {
  private var position = 0L

  override fun protectedRead(sink: Buffer, byteCount: Long): Long {
    val remainingBytes = byteCount.coerceAtMost(size - position)
    val readBytes = wrapped.read(sink, remainingBytes)

    return if (readBytes == -1L) {
      -1L
    } else {
      position += readBytes
      readBytes
    }
  }

  override fun exhausted(): Exhausted = positionExhausted(position)

  override fun protectedClose() = wrapped.close()

  override fun toString(): String = "fixedLength($wrapped, $size)"
}
