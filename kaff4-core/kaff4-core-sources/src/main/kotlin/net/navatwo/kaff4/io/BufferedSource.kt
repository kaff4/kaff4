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

  fun skip(byteCount: Long): Long {
    return Buffer().use { buffer ->
      var remainingBytes = byteCount
      var readBytes = okioSource.read(buffer, remainingBytes)
      if (readBytes == -1L) return@use -1L // exhausted

      remainingBytes -= readBytes
      do {
        readBytes = okioSource.read(buffer, remainingBytes)

        if (readBytes == -1L) break

        remainingBytes -= readBytes
      } while (remainingBytes != 0L)

      byteCount - remainingBytes
    }
  }

  override fun timeout(): Timeout = source.timeout()

  internal fun checkTimeoutAndClosed() {
    timeout().throwIfReached()
    check(!closed) { "closed" }
  }
}

fun Source.buffer(): BufferedSource = BufferedSource(this)
