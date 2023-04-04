package com.github.nava2.aff4.container

import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.Aff4Image
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4StreamOpener

internal class RealAff4Image(
  override val aff4Model: Aff4Model,
  override val streamOpener: Aff4StreamOpener,
  override val containers: List<Aff4Container>,
) : Aff4Image {
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
