package net.navatwo.kaff4.io

import okio.Buffer
import okio.Timeout
import okio.buffer

class BufferedSource(
  val source: Source,
) : Source {

  @Volatile
  private var closed = false

  internal val okioSource = source.asOkio().buffer()

  override fun close() {
    if (closed) return

    closed = true
    source.close()
    okioSource.close()
  }

  override fun read(sink: Buffer, byteCount: Long): Long {
    checkTimeoutAndClosed()

    return okioSource.read(sink, byteCount)
  }

  override fun skip(byteCount: Long): Long {
    checkTimeoutAndClosed()
    require(byteCount >= 0) { "byteCount >= 0" }

    if (byteCount == 0L) return 0

    val skippedBytes = source.skip(byteCount)
    if (skippedBytes != -1L) {
      okioSource.buffer.clear()
    }

    return skippedBytes
  }

  override fun timeout(): Timeout = source.timeout()

  internal fun checkTimeoutAndClosed() {
    timeout().throwIfReached()
    check(!closed) { "closed" }
  }
}

fun Source.buffer(): BufferedSource = BufferedSource(this)
