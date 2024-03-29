package net.navatwo.kaff4.io

import net.navatwo.kaff4.io.Source.Exhausted
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

  final override fun timeout(): Timeout = timeout

  final override fun close() {
    if (closed) return

    closed = true
    protectedClose()
  }

  protected abstract fun protectedRead(sink: Buffer, byteCount: Long): Long

  protected abstract fun protectedClose()

  protected fun checkClosedOrTimedOut() {
    check(!closed) { "closed" }
    timeout.throwIfReached()
  }
}
