package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.io.fixedLength
import com.github.nava2.aff4.meta.rdf.model.MapStream
import com.github.nava2.aff4.model.Aff4StreamOpener
import com.github.nava2.aff4.streams.Aff4Stream
import com.github.nava2.aff4.streams.SourceProviderWithRefCounts
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.buffer
import java.io.Closeable

class Aff4MapStream @AssistedInject internal constructor(
  private val aff4StreamOpener: Aff4StreamOpener,
  private val mapStreamMapReader: MapStreamMapReader,
  @Assisted val mapStream: MapStream,
) : Aff4Stream {
  private val sourceProviderWithRefCounts = SourceProviderWithRefCounts(::readAt)

  private val map by lazy { mapStreamMapReader.loadMap(mapStream) }

  val size: Long = mapStream.size

  private var position: Long = 0L

  private var currentSource: CurrentSourceInfo? = null

  override fun source(position: Long): Source = sourceProviderWithRefCounts.source(position)

  override fun close() {
    currentSource?.close()
    currentSource = null

    sourceProviderWithRefCounts.close()
  }

  private fun readAt(readPosition: Long, sink: Buffer, byteCount: Long): Long {
    moveTo(readPosition)

    // we are exhausted
    if (position == size) return -1L

    val nextEntry = map.query(position, byteCount).firstOrNull() ?: return -1

    val maxBytesToRead = byteCount.coerceAtMost(nextEntry.length)

    val readSource = getAndUpdateCurrentSourceIfChanged(nextEntry)

    val bytesRead = readSource.read(sink, maxBytesToRead)
    check(bytesRead >= 0) {
      // because of how we read these targets by capping their read size to the entry.length, we *should* never read
      // them when they are exhausted.
      "Read too much of target [${nextEntry.targetIRI}] - $nextEntry - $mapStream"
    }

    position += bytesRead.coerceAtLeast(0)

    return bytesRead
  }

  private fun getAndUpdateCurrentSourceIfChanged(nextEntry: MapStreamEntry): BufferedSource {
    val currentSource = currentSource

    if (currentSource?.entry == nextEntry) {
      return currentSource.source
    }

    resetCurrentSource()

    val targetStream = aff4StreamOpener.openStream(nextEntry.targetIRI)
    return targetStream.source(nextEntry.targetOffset)
      .fixedLength(nextEntry.length)
      .buffer()
  }

  private fun moveTo(newPosition: Long) {
    val cappedPosition = newPosition.coerceAtMost(size)
    if (cappedPosition == position) return

    val currentSource = this.currentSource

    when {
      currentSource == null -> Unit
      cappedPosition > position && currentSource.entry.let { cappedPosition in it } -> {
        // try and skip forwards if its a small gap
        currentSource.source.skip(cappedPosition - position)
      }
      else -> resetCurrentSource()
    }

    position = cappedPosition
  }

  private fun resetCurrentSource() {
    currentSource?.close()
    currentSource = null
  }

  private data class CurrentSourceInfo(
    val entry: MapStreamEntry,
    val source: BufferedSource,
  ) : Closeable by source

  interface Loader : Aff4Stream.Loader<MapStream, Aff4MapStream>
}
