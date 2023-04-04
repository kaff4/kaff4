package net.navatwo.kaff4.container

import net.navatwo.kaff4.model.Aff4Container
import net.navatwo.kaff4.model.Aff4Image
import net.navatwo.kaff4.model.Aff4Model
import net.navatwo.kaff4.model.Aff4StreamOpener

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
