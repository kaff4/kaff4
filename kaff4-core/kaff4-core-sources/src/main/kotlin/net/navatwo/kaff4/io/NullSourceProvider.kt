package net.navatwo.kaff4.io

import okio.Buffer
import okio.Timeout

internal object NullSourceProvider : SourceProvider<Source> {
  override fun source(position: Long, timeout: Timeout): Source {
    require(position >= 0L)
    return NullSource.create(timeout)
  }
}

private class NullSource private constructor(timeout: Timeout) : AbstractSource(timeout) {
  override fun protectedClose() = Unit

  override fun protectedRead(sink: Buffer, byteCount: Long): Long = -1L
  override fun protectedSkip(byteCount: Long): Long = -1L

  override fun exhausted(): Exhausted = Exhausted.EXHAUSTED

  override fun toString(): String {
    return "null(size = 0L)"
  }

  companion object {
    private val noTimeoutNullSource by lazy(LazyThreadSafetyMode.NONE) { NullSource(Timeout.NONE) }

    fun create(timeout: Timeout): NullSource = if (timeout == Timeout.NONE) {
      noTimeoutNullSource
    } else {
      NullSource(timeout)
    }
  }
}
