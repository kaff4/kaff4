package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.io.applyAndCloseOnThrow
import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.limit
import com.github.nava2.aff4.model.Aff4StreamOpener
import com.github.nava2.aff4.model.rdf.MapStream
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import java.io.Closeable

internal class Aff4MapStreamSource(
  private val aff4StreamOpener: Aff4StreamOpener,
  private val mapStream: MapStream,
  private val map: MapStreamMap,
  private var position: Long,
  private val timeout: Timeout,
) : Source {
  private val size = mapStream.size

  private var currentSource: CurrentSourceInfo? = null

  override fun read(sink: Buffer, byteCount: Long): Long {
    // we are exhausted
    if (position == size) return -1L

    val entryToRead = map.query(position, byteCount).firstOrNull() ?: return -1

    val maxBytesToRead = byteCount.coerceAtMost(entryToRead.length - (position - entryToRead.mappedOffset))

    val readSource = getAndUpdateCurrentSourceIfChanged(position, timeout, entryToRead)

    val bytesRead = readSource.read(sink, maxBytesToRead)
    check(bytesRead >= 0) {
      // because of how we read these targets by capping their read size to the entry.length, we *should* never read
      // them when they are exhausted.
      "Read too much of target [${entryToRead.targetArn}] - $entryToRead - $mapStream"
    }

    position += bytesRead

    return bytesRead
  }

  override fun timeout(): Timeout = timeout

  override fun close() {
    resetCurrentSource()
  }

  private fun getAndUpdateCurrentSourceIfChanged(
    nextPosition: Long,
    timeout: Timeout,
    entryToRead: MapStreamEntry,
  ): BufferedSource {
    val currentSource = currentSource

    if (currentSource?.entry == entryToRead) {
      return currentSource.source
    }

    resetCurrentSource()

    val targetSourceProvider = aff4StreamOpener.openStream(entryToRead.targetArn)
    val targetSource = targetSourceProvider
      .limit(entryToRead.length)
      .buffer()
      .source(entryToRead.targetOffset, timeout)
      .applyAndCloseOnThrow {
        if (nextPosition != entryToRead.mappedOffset) {
          skip(entryToRead.mappedOffset - nextPosition)
        }
      }

    this.currentSource = CurrentSourceInfo(entryToRead, targetSource)

    return targetSource
  }

  private fun resetCurrentSource() {
    currentSource?.close()
    currentSource = null
  }

  private data class CurrentSourceInfo(
    val entry: MapStreamEntry,
    val source: BufferedSource,
  ) : Closeable by source
}
