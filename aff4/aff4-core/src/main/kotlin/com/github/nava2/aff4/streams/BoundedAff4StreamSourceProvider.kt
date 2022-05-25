package com.github.nava2.aff4.streams

import com.github.nava2.aff4.io.limit
import com.github.nava2.aff4.model.Aff4StreamSourceProvider
import okio.Source
import okio.Timeout

internal class BoundedAff4StreamSourceProvider(
  private val delegate: Aff4StreamSourceProvider,
  private val delegateOffset: Long,
  override val size: Long,
) : Aff4StreamSourceProvider {
  init {
    check(delegateOffset in 0 until delegate.size)
  }

  private val relativeBounds = 0 until size

  override fun source(position: Long, timeout: Timeout): Source {
    require(position in relativeBounds)

    val delegatePosition = delegateOffset + position
    val remainingLength = size - position

    return delegate.limit(remainingLength).source(delegatePosition, timeout)
  }

  override fun close() = Unit

  override fun toString(): String {
    return "Bounded($delegate)"
  }
}
