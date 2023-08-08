package net.navatwo.kaff4.io

import okio.Buffer
import okio.buffer

class BufferedSource(
  val source: Source,
) : AbstractSource(source.timeout()) {

  private val okioBuffer = source.asOkio().buffer()

  override fun asOkio(): okio.BufferedSource {
    checkClosedOrTimedOut()
    return okioBuffer
  }

  override fun protectedClose() {
    source.close()
    okioBuffer.close()
  }

  override fun protectedRead(sink: Buffer, byteCount: Long): Long = okioBuffer.read(sink, byteCount)

  fun skipFully(byteCount: Long) {
    checkClosedOrTimedOut()
    okioBuffer.skip(byteCount)
  }
}

fun Source.buffer(): BufferedSource = BufferedSource(this)
