package com.github.nava2.aff4.io

import okio.BufferedSink
import okio.BufferedSource

internal fun BufferedSource.exhaust(sink: BufferedSink, byteCount: Long): Long {
  var firstRead = true
  var bytesRemaining = byteCount
  do {
    val bytesRead = read(sink.buffer, bytesRemaining)
    if (bytesRead == -1L) {
      // On first read, we return -1 to follow the source pattern
      return if (firstRead) -1 else byteCount - bytesRemaining
    }

    bytesRemaining -= bytesRead
    firstRead = false
  } while (bytesRemaining > 0 && bytesRead >= 0)

  return byteCount - bytesRemaining
}
