package net.navatwo.kaff4.io

import okio.Buffer

fun okio.Source.asKAff4(): OkioWrappedSource {
  return OkioWrappedSource(this)
}

class OkioWrappedSource internal constructor(val okio: okio.Source) : AbstractSource(okio.timeout()) {
  override fun protectedRead(sink: Buffer, byteCount: Long): Long = okio.read(sink, byteCount)

  override fun protectedClose() = okio.close()
}
