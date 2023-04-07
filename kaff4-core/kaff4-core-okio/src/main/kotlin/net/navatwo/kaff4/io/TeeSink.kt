package net.navatwo.kaff4.io

import okio.Buffer
import okio.IOException
import okio.Sink
import okio.Timeout

/**
 * Duplicates any calls to [write] to all [sinks].
 */
data class TeeSink(
  private val sinks: List<Sink>,
  private val timeout: Timeout,
  /** If true, closes all sinks when this closes */
  private val closeAllOnClose: Boolean = false,
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

  override fun close() {
    if (!closeAllOnClose) return

    for (sink in sinks) {
      sink.close()
    }
  }

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
