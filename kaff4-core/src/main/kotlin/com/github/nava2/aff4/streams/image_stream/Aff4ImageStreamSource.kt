package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.io.applyAndCloseOnThrow
import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.model.rdf.ImageStream
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import java.io.Closeable

internal class Aff4ImageStreamSource(
  private val aff4ImageBevies: Aff4ImageBevies,
  private val imageStream: ImageStream,
  private var position: Long,
  private val timeout: Timeout,
) : Source {
  private val bevyMaxSize = imageStream.bevyMaxSize

  private val size = imageStream.size

  init {
    require(position <= size) { "position [$position] <=  imageStream.size [$size] " }
  }

  private var currentSource: CurrentSourceInfo? = null

  @Volatile
  private var closed = false

  override fun read(sink: Buffer, byteCount: Long): Long {
    check(!closed) { "closed" }

    // we are exhausted
    if (position == size) return -1L

    val nextBevyIndex = position.floorDiv(bevyMaxSize).toInt()
    val maxBytesToRead = byteCount.coerceAtMost(size - position)

    val readSource = getAndUpdateCurrentSourceIfChanged(nextBevyIndex, timeout)

    val bytesRead = readSource.read(sink, maxBytesToRead)
    check(bytesRead >= 0) {
      // because of how we read these bevies by capping their read sizes, we should never read them when they
      // are exhausted but do a full read to their end point.
      "Read too much of bevy [$nextBevyIndex] - $imageStream"
    }

    position += bytesRead

    return bytesRead
  }

  override fun close() {
    if (closed) return
    synchronized(this) {
      if (closed) return
      closed = true
    }

    currentSource?.close()
  }

  override fun timeout(): Timeout = timeout

  private fun getAndUpdateCurrentSourceIfChanged(nextBevyIndex: Int, timeout: Timeout): Source {
    val currentSource = currentSource

    if (currentSource?.bevyIndex == nextBevyIndex) {
      return currentSource.source
    }

    currentSource?.close()
    this.currentSource = null

    val bevyPosition = position % bevyMaxSize

    return aff4ImageBevies.getOrLoadBevy(nextBevyIndex).buffer().source(bevyPosition, timeout).applyAndCloseOnThrow {
      val sourceInfo = CurrentSourceInfo(nextBevyIndex, this)

      this@Aff4ImageStreamSource.currentSource = sourceInfo
    }
  }

  private data class CurrentSourceInfo(
    val bevyIndex: Int,
    val source: BufferedSource,
  ) : Closeable by source
}
