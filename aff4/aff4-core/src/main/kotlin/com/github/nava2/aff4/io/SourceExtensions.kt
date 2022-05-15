package com.github.nava2.aff4.io

import okio.Buffer
import okio.Source

internal fun Source.exhaust(sink: Buffer, byteCount: Long): Long {
  if (byteCount == 0L) return 0L

  var bytesRemaining = byteCount
  do {
    val bytesRead = read(sink, bytesRemaining)
    if (bytesRead != -1L) {
      bytesRemaining -= bytesRead
    }
  } while (bytesRemaining > 0 && bytesRead >= 0)

  return byteCount - bytesRemaining
}
