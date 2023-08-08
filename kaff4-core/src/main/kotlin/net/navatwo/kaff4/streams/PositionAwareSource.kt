package net.navatwo.kaff4.streams

import net.navatwo.kaff4.io.Sized
import net.navatwo.kaff4.io.Source
import net.navatwo.kaff4.io.Source.Exhausted.Companion.positionExhausted

internal interface PositionAwareSource : Source {
  val position: Long

  companion object {
    fun <T> T.currentlyExhausted(): Source.Exhausted where T : Sized, T : PositionAwareSource = positionExhausted(
      position
    )
  }
}
