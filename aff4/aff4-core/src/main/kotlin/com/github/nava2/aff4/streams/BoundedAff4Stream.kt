package com.github.nava2.aff4.streams

import com.github.nava2.aff4.io.fixedLength
import com.github.nava2.aff4.model.Aff4Stream
import okio.Source

internal class BoundedAff4Stream(
  private val delegate: Aff4Stream,
  private val delegateOffset: Long,
  override val size: Long,
) : Aff4Stream {
  init {
    check(delegateOffset in 0 until delegate.size)
  }

  private val relativeBounds = 0 until size

  override fun source(position: Long): Source {
    check(position in relativeBounds)

    val delegatePosition = delegateOffset + position
    val remainingLength = size - position

    return delegate.source(delegatePosition).fixedLength(remainingLength)
  }

  override fun close() = Unit

  override fun toString(): String {
    return "Bounded($delegate)"
  }
}
