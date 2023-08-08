package net.navatwo.kaff4.streams.image_stream

import net.navatwo.kaff4.io.AbstractSource
import net.navatwo.kaff4.io.BufferedSource
import net.navatwo.kaff4.io.Source
import net.navatwo.kaff4.io.applyAndCloseOnThrow
import net.navatwo.kaff4.io.buffer
import net.navatwo.kaff4.model.rdf.ImageStream
import okio.Buffer
import okio.Timeout
import java.io.Closeable

internal class Aff4ImageStreamSource(
  private val aff4ImageBevies: Aff4ImageBevies,
  private val imageStream: ImageStream,
  private var position: Long,
  timeout: Timeout,
) : AbstractSource(timeout) {
  private val bevyMaxSize = imageStream.bevyMaxSize

  private val size = imageStream.size

  init {
    require(position <= size) { "position [$position] <=  imageStream.size [$size] " }
  }

  private var currentSource: CurrentSourceInfo? = null

  override fun protectedRead(sink: Buffer, byteCount: Long): Long {
    val nextBevyIndex = position.floorDiv(bevyMaxSize).toInt()
    val maxBytesToRead = byteCount.coerceAtMost(size - position)

    val readSource = getAndUpdateCurrentSourceIfChanged(nextBevyIndex)

    val bytesRead = readSource.read(sink, maxBytesToRead)
    check(bytesRead >= 0) {
      // because of how we read these bevies by capping their read sizes, we should never read them when they
      // are exhausted but do a full read to their end point.
      "Read too much of bevy [$nextBevyIndex] - $imageStream"
    }

    position += bytesRead

    return bytesRead
  }

  override fun protectedSkip(byteCount: Long): Long {
    val maxBytesToRead = byteCount.coerceAtMost(size - position)
    position += maxBytesToRead

    return maxBytesToRead
  }

  override fun protectedClose() {
    currentSource?.close()
  }

  override fun exhausted() = Exhausted.from(position == size)

  private fun getAndUpdateCurrentSourceIfChanged(nextBevyIndex: Int): Source {
    val currentSource = currentSource

    if (currentSource?.bevyIndex == nextBevyIndex) {
      return currentSource.source
    }

    currentSource?.close()
    this.currentSource = null

    val bevyPosition = position % bevyMaxSize

    return aff4ImageBevies.getOrLoadBevy(nextBevyIndex).buffer().source(bevyPosition, timeout()).applyAndCloseOnThrow {
      val sourceInfo = CurrentSourceInfo(nextBevyIndex, this)

      this@Aff4ImageStreamSource.currentSource = sourceInfo
    }
  }

  private data class CurrentSourceInfo(
    val bevyIndex: Int,
    val source: BufferedSource,
  ) : Closeable by source
}
