package com.github.nava2.aff4.streams

import com.github.nava2.aff4.io.SourceProvider
import okio.Buffer
import okio.Source
import okio.Timeout

internal class SourceProviderWithRefCounts(
  private val sourceDelegate: SourceDelegate,
) : SourceProvider<Source>, AutoCloseable {
  @Volatile
  private var closed = false
  private var sourcesOutstanding = 0L

  @Synchronized
  override fun source(position: Long, timeout: Timeout): Source {
    check(!closed)
    require(position >= 0)

    sourcesOutstanding += 1

    return RefCountedOffsetSource(sourceDelegate, position, timeout) {
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
    fun readAt(readPosition: Long, timeout: Timeout, sink: Buffer, byteCount: Long): Long
  }

  private class RefCountedOffsetSource(
    private val sourceDelegate: SourceDelegate,
    private var sourcePosition: Long,
    private val timeout: Timeout,
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
      timeout.throwIfReached()

      val result = sourceDelegate.readAt(sourcePosition, timeout, sink, byteCount)
      if (result == -1L) return -1

      sourcePosition += result

      return result
    }

    override fun timeout(): Timeout = timeout

    override fun toString(): String {
      return "offsetSource($sourceDelegate)"
    }
  }
}
