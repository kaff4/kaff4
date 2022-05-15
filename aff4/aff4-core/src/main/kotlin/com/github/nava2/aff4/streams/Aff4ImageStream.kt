package com.github.nava2.aff4.streams

import com.github.nava2.aff4.io.exhaust
import com.github.nava2.aff4.meta.rdf.model.ImageStream
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Source
import okio.buffer

class Aff4ImageStream internal constructor(
  bevyFactory: Bevy.Factory,
  fileSystem: FileSystem,
  imageStreamConfig: ImageStream,
) : AutoCloseable {
  private val sourceProviderWithRefCounts = SourceProviderWithRefCounts(::readAt)

  private val aff4ImageBevies = Aff4ImageBevies(bevyFactory, fileSystem, imageStreamConfig)
  private val chunksInSegment = imageStreamConfig.chunksInSegment
  private val chunkSize = imageStreamConfig.chunkSize
  private val bevySize = chunksInSegment * chunkSize

  val size: Long = imageStreamConfig.size

  private var position: Long = 0L

  private var currentSource: BufferedSource? = null

  fun source(position: Long): Source {
    return sourceProviderWithRefCounts.source(position)
  }

  override fun close() {
    currentSource?.close()
    currentSource = null

    sourceProviderWithRefCounts.close()
    aff4ImageBevies.close()
  }

  private fun readAt(readPosition: Long, sink: Buffer, byteCount: Long): Long {
    val earlyReturn = when {
      position == size -> -1L
      byteCount == 0L -> 0L
      else -> null
    }
    if (earlyReturn != null) return earlyReturn

    moveTo(readPosition)

    var currentBevyIndex = position.floorDiv(bevySize).toInt()
    val maxBytesToRead = byteCount.coerceAtMost(size - position)
    var bytesRemaining = maxBytesToRead

    do {
      val nextBevyIndex = position.floorDiv(bevySize).toInt()

      currentSource = if (currentSource == null || currentBevyIndex != nextBevyIndex) {
        currentSource?.close()

        val bevyPosition = position % bevySize
        aff4ImageBevies.getOrLoadBevy(nextBevyIndex).source(bevyPosition).buffer()
      } else {
        currentSource
      }

      val bytesRead = currentSource!!.exhaust(sink, bytesRemaining)

      bytesRemaining -= bytesRead
      position += bytesRead

      currentBevyIndex = nextBevyIndex
    } while (bytesRemaining > 0 && position < size)

    return maxBytesToRead - bytesRemaining
  }

  private fun moveTo(newPosition: Long) {
    when {
      newPosition == position -> return
      newPosition > position && newPosition - position < chunkSize -> {
        // try and skip forwards if its a small gap
        currentSource?.skip(newPosition.coerceAtMost(size) - position)
      }
      else -> {
        currentSource?.close()
        currentSource = null
      }
    }

    position = newPosition
  }
}
