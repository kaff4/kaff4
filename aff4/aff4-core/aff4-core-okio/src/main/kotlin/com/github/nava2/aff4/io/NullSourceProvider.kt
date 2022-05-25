package com.github.nava2.aff4.io

import okio.Buffer
import okio.Source
import okio.Timeout

internal object NullSourceProvider : SourceProvider<Source> {
  override fun source(position: Long, timeout: Timeout): Source {
    require(position >= 0L)
    return NullSource.create(timeout)
  }
}

fun nullSource(timeout: Timeout = Timeout.NONE): Source = NullSource.create(timeout)

private class NullSource private constructor(private val timeout: Timeout) : Source {
  override fun close() = Unit

  override fun read(sink: Buffer, byteCount: Long): Long = -1L

  override fun timeout(): Timeout = timeout

  override fun toString(): String {
    return "null(size = 0L)"
  }

  companion object {
    private val noTimeoutNullSource by lazy(LazyThreadSafetyMode.NONE) { NullSource(Timeout.NONE) }

    fun create(timeout: Timeout): NullSource = if (timeout == Timeout.NONE) {
      noTimeoutNullSource
    } else {
      NullSource(timeout)
    }
  }
}
