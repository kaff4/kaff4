package net.navatwo.kaff4.io

import okio.Buffer
import okio.Sink

internal data class ProgressSink(
  private val sink: Sink,
) : Sink by sink {
  private val listeners = mutableListOf<Listener>()

  fun addListener(listener: Listener) {
    listeners += listener
  }

  override fun write(source: Buffer, byteCount: Long) {
    sink.write(source, byteCount)

    for (reporter in listeners) {
      reporter.onWrite(byteCount)
    }
  }

  interface Listener {
    fun onWrite(bytesWritten: Long)
  }
}
