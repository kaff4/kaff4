package com.github.nava2.aff4.io

import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer

fun List<SourceProvider<Source>>.concatLazily(): SourceProvider<Source> {
  return LazyConcatSourceProvider(this)
}

class LazyConcatSourceProvider internal constructor(
  private val lazySources: List<SourceProvider<Source>>,
) : SourceProvider<Source> {
  override fun source(position: Long, timeout: Timeout): Source {
    require(position >= 0L)

    val lazySource = LazyConcatSource(lazySources, timeout)
    if (position == 0L) return lazySource

    return lazySource.buffer().applyAndCloseOnThrow { skip(position) }
  }
}

private class LazyConcatSource(
  private val lazySources: List<SourceProvider<Source>>,
  private val timeout: Timeout,
) : Source {
  private val iter = lazySources.iterator()
  private var current: Source = iter.next().source(timeout = timeout)

  override fun read(sink: Buffer, byteCount: Long): Long {
    timeout.throwIfReached()

    val bytesRead = current.read(sink, byteCount)

    return when {
      bytesRead == -1L && !iter.hasNext() -> -1L
      bytesRead == -1L && iter.hasNext() -> {
        current.close()
        current = iter.next().source(timeout = timeout)

        read(sink, byteCount)
      }

      else -> bytesRead
    }
  }

  override fun close() = current.close()
  override fun timeout(): Timeout = timeout

  override fun toString(): String = "lazyConcatSource(Sources(${lazySources.size}))"
}
