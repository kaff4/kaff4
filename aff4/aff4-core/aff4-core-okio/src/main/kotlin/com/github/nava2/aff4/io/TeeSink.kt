package com.github.nava2.aff4.io

import okio.Buffer
import okio.IOException
import okio.Sink
import okio.Timeout

/**
 * Duplicates any calls to [write] to all [sinks].
 */
class TeeSink(
  val sinks: List<Sink>,
  private val timeout: Timeout,
) : Sink {
  override fun write(source: Buffer, byteCount: Long) {
    timeout.throwIfReached()

    for ((idx, nextSink) in sinks.withIndex()) {
      if (idx == sinks.lastIndex) {
        nextSink.write(source, byteCount)
      } else {
        source.copy().use { nextSource ->
          nextSink.write(nextSource, byteCount)
        }
      }
    }
  }

  override fun close() = Unit

  override fun flush() {
    var caughtEx: Exception? = null

    for (sink in sinks) {
      try {
        sink.flush()
      } catch (ex: IOException) {
        if (caughtEx == null) {
          caughtEx = ex
        } else {
          caughtEx.addSuppressed(ex)
        }
      }
    }

    if (caughtEx != null) {
      throw caughtEx
    }
  }

  override fun timeout(): Timeout = timeout

  override fun toString(): String {
    return "tee(${sinks.joinToString(", ")})"
  }
}
