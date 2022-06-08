package com.github.nava2.aff4.container

import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4StreamOpener
import java.io.Closeable

class Aff4Container internal constructor(
  val aff4Model: Aff4Model,
  val streamOpener: Aff4StreamOpener,
) : Closeable {
  @Volatile
  private var closed = false

  override fun close() {
    if (closed) return
    synchronized(this) {
      if (closed) return
      closed = true
    }

    streamOpener.close()
    aff4Model.close()
  }
}
