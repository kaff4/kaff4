package com.github.nava2.aff4.streams

import okio.Buffer
import okio.Source
import okio.Timeout

internal class SourceProviderWithRefCounts(
  private val sourceDelegate: SourceDelegate,
) : AutoCloseable {
  @Volatile
  private var closed = false
  private var sourcesOutstanding = 0L

  @Synchronized
  fun source(position: Long): Source {
    check(!closed)
    require(position >= 0)

    sourcesOutstanding += 1

    return RefCountedOffsetSource(sourceDelegate, position) {
      synchronized(this) {
        sourcesOutstanding -= 1
      }
    }
  }

  @Synchronized
  override fun close() {
    if (closed) return

    check(sourcesOutstanding == 0L) {
      "Sources were created and not freed: $sourcesOutstanding"
    }

    closed = true
  }

  internal interface SourceDelegate {
    fun readAt(readPosition: Long, sink: Buffer, byteCount: Long): Long
  }

  private class RefCountedOffsetSource(
    private val sourceDelegate: SourceDelegate,
    private var sourcePosition: Long,
    private val onCloseSource: (Source) -> Unit,
  ) : Source {
    @Volatile
    private var closed = false

    override fun close() {
      if (closed) return

      synchronized(this) {
        if (closed) return
        closed = true
      }

      onCloseSource(this)
    }

    override fun read(sink: Buffer, byteCount: Long): Long {
      check(!closed)

      val result = sourceDelegate.readAt(sourcePosition, sink, byteCount)
      if (result == -1L) return -1

      sourcePosition += result

      return result
    }

    override fun timeout(): Timeout = Timeout.NONE

    override fun toString(): String {
      return "offsetSource($sourceDelegate)"
    }
  }
}
