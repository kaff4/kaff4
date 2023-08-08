package net.navatwo.kaff4.io

import okio.Buffer
import okio.Timeout

fun okio.Source.asKAff4(): OkioWrappedSource {
  return OkioWrappedSource(this)
}

class OkioWrappedSource internal constructor(val wrapped: okio.Source) : Source {
  override fun read(sink: Buffer, byteCount: Long): Long = wrapped.read(sink, byteCount)

  override fun timeout(): Timeout = wrapped.timeout()

  override fun close() = wrapped.close()
}
