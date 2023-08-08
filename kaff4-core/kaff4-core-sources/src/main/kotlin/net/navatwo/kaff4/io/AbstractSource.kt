package net.navatwo.kaff4.io

import okio.Buffer
import okio.Timeout

abstract class AbstractSource protected constructor(
  private val timeout: Timeout,
) : Source {
  @Volatile
  private var closed = false

  final override fun read(sink: Buffer, byteCount: Long): Long {
    checkClosedOrTimedOut()
    require(byteCount >= 0) { "byteCount >= 0" }

    if (exhausted() == Exhausted.EXHAUSTED) return -1L

    return protectedRead(sink, byteCount)
  }

  final override fun skip(byteCount: Long): Long {
    checkClosedOrTimedOut()
    require(byteCount >= 0) { "byteCount >= 0" }

    if (exhausted() == Exhausted.EXHAUSTED) return -1L
    if (byteCount == 0L) return 0L

    return protectedSkip(byteCount)
  }

  final override fun timeout(): Timeout = timeout

  final override fun close() {
    if (closed) return

    closed = true
    protectedClose()
  }

  protected abstract fun protectedRead(sink: Buffer, byteCount: Long): Long
  protected abstract fun protectedSkip(byteCount: Long): Long

  protected abstract fun protectedClose()

  protected abstract fun exhausted(): Exhausted

  protected enum class Exhausted {
    HAS_VALUES,
    EXHAUSTED,
    UNKNOWN,
    ;

    companion object {
      fun from(exhausted: Boolean) = if (exhausted) EXHAUSTED else HAS_VALUES
    }
  }

  protected fun checkClosedOrTimedOut() {
    check(!closed) { "closed" }
    timeout.throwIfReached()
  }
}
