package net.navatwo.kaff4.io

import net.navatwo.kaff4.api.InternalApi
import net.navatwo.kaff4.io.Source.Exhausted
import okio.Buffer
import okio.Timeout

@InternalApi
fun List<SourceProvider<Source>>.concatLazily(): SourceProvider<Source> {
  return LazyConcatSourceProvider(this)
}

internal class LazyConcatSourceProvider(
  private val lazySources: List<SourceProvider<Source>>,
) : SourceProvider<Source> {
  override fun source(position: Long, timeout: Timeout): Source {
    require(position >= 0L)

    val lazySource = LazyConcatSource(lazySources, timeout)
    if (position == 0L) return lazySource

    return lazySource.buffer().applyAndCloseOnThrow { skipFully(position) }
  }
}

private class LazyConcatSource(
  private val lazySources: List<SourceProvider<Source>>,
  private val timeout: Timeout,
) : AbstractSource(timeout) {
  private val iter = lazySources.iterator()
  private var current: Source = iter.next().source(timeout = timeout)

  override fun protectedRead(sink: Buffer, byteCount: Long): Long {
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

  override fun exhausted(): Exhausted {
    return when (val currentExhaustion = current.exhausted()) {
      Exhausted.HAS_VALUES, Exhausted.UNKNOWN -> currentExhaustion
      // If current is EXHAUSTED, we check if there's any more content available instead of returning [EXHAUSTED]
      Exhausted.EXHAUSTED -> Exhausted.hasRemaining(iter.hasNext())
    }
  }

  override fun protectedClose() = current.close()

  override fun toString(): String = "lazyConcatSource(Sources(${lazySources.size}))"
}
