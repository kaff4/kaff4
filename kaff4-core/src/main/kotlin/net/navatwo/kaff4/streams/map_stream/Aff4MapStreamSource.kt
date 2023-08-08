package net.navatwo.kaff4.streams.map_stream

import net.navatwo.kaff4.io.AbstractSource
import net.navatwo.kaff4.io.BufferedSource
import net.navatwo.kaff4.io.applyAndCloseOnThrow
import net.navatwo.kaff4.io.buffer
import net.navatwo.kaff4.io.limit
import net.navatwo.kaff4.model.Aff4StreamOpener
import net.navatwo.kaff4.model.rdf.MapStream
import okio.Buffer
import okio.Timeout
import java.io.Closeable

internal class Aff4MapStreamSource(
  private val aff4StreamOpener: Aff4StreamOpener,
  private val mapStream: MapStream,
  private val map: MapStreamMap,
  private var position: Long,
  timeout: Timeout,
) : AbstractSource(timeout) {
  private val size = mapStream.size

  private var currentSource: CurrentSourceInfo? = null

  override fun protectedRead(sink: Buffer, byteCount: Long): Long {
    val entryToRead = map.query(position, byteCount).firstOrNull() ?: return -1

    val maxBytesToRead = byteCount.coerceAtMost(entryToRead.length - (position - entryToRead.mappedOffset))

    val readSource = getAndUpdateCurrentSourceIfChanged(position, entryToRead)

    val bytesRead = readSource.read(sink, maxBytesToRead)
    check(bytesRead >= 0) {
      // because of how we read these targets by capping their read size to the entry.length, we *should* never read
      // them when they are exhausted.
      "Read too much of target [${entryToRead.targetArn}] - $entryToRead - $mapStream"
    }

    position += bytesRead

    return bytesRead
  }

  override fun protectedClose() {
    resetCurrentSource()
  }

  override fun exhausted() = Exhausted.from(position == size)

  private fun getAndUpdateCurrentSourceIfChanged(
    nextPosition: Long,
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
      .source(entryToRead.targetOffset, timeout())
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
