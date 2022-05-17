package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.meta.rdf.model.Hash
import com.github.nava2.aff4.meta.rdf.model.ImageStream
import com.github.nava2.aff4.streams.Aff4Stream
import com.github.nava2.aff4.streams.Hashing.computeLinearHashes
import com.github.nava2.aff4.streams.SourceProviderWithRefCounts
import com.github.nava2.aff4.streams.VerifiableStream
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.buffer
import java.io.Closeable

class Aff4ImageStream @AssistedInject internal constructor(
  aff4ImageBeviesFactory: Aff4ImageBevies.Factory,
  @Assisted private val imageStreamConfig: ImageStream,
) : VerifiableStream, Aff4Stream {

  private val sourceProviderWithRefCounts = SourceProviderWithRefCounts(::readAt)

  private val aff4ImageBevies = aff4ImageBeviesFactory.create(imageStreamConfig)
  private val chunksInSegment = imageStreamConfig.chunksInSegment
  private val chunkSize = imageStreamConfig.chunkSize
  private val bevySize = chunksInSegment * chunkSize

  val size: Long = imageStreamConfig.size

  private var position: Long = 0L

  private var currentSource: CurrentSourceInfo? = null

  private var verificationResult: VerifiableStream.Result? = null

  override fun source(position: Long): Source {
    return sourceProviderWithRefCounts.source(position)
  }

  override fun verify(): VerifiableStream.Result {
    if (verificationResult != null) {
      return verificationResult!!
    }

    val failedHashes = mutableListOf<Pair<String, Hash>>()

    val linearHashes = source(0).buffer().use { s ->
      s.computeLinearHashes(imageStreamConfig.linearHashes)
    }

    for ((expectedHash, actualHash) in linearHashes) {
      if (expectedHash.hash != actualHash) {
        failedHashes += "Linear ${expectedHash.name}" to expectedHash
      }
    }

    return if (failedHashes.isNotEmpty()) {
      VerifiableStream.Result.Failed(failedHashes)
    } else {
      VerifiableStream.Result.Success
    }
  }

  override fun close() {
    currentSource?.close()
    currentSource = null

    sourceProviderWithRefCounts.close()
    aff4ImageBevies.close()
  }

  private fun readAt(readPosition: Long, sink: Buffer, byteCount: Long): Long {
    moveTo(readPosition)

    // we are exhausted
    if (position == size) return -1L

    val nextBevyIndex = position.floorDiv(bevySize).toInt()
    val maxBytesToRead = byteCount.coerceAtMost(size - position)

    val readSource = getAndUpdateCurrentSourceIfChanged(nextBevyIndex)

    val bytesRead = readSource.read(sink, maxBytesToRead)

    return if (position != size) {
      position += bytesRead.coerceAtLeast(0)

      bytesRead
    } else {
      -1L
    }
  }

  private fun getAndUpdateCurrentSourceIfChanged(nextBevyIndex: Int): BufferedSource {
    val currentSource = currentSource

    if (currentSource?.bevyIndex == nextBevyIndex) {
      return currentSource.source
    }

    currentSource?.close()
    this.currentSource = null

    val bevyPosition = position % bevySize

    var nextSource: Source? = null
    var nextBufferedSource: BufferedSource? = null

    return try {
      nextSource = aff4ImageBevies.getOrLoadBevy(nextBevyIndex).source(bevyPosition)
      nextBufferedSource = nextSource.buffer()

      val sourceInfo = CurrentSourceInfo(nextBevyIndex, nextBufferedSource)

      this.currentSource = sourceInfo

      nextBufferedSource
    } catch (@Suppress("TooGenericExceptionCaught") ex: Exception) {
      nextSource?.close()
      nextBufferedSource?.close()

      throw ex
    }
  }

  private fun moveTo(newPosition: Long) {
    val cappedPosition = newPosition.coerceAtMost(size)
    when {
      cappedPosition == position -> return
      cappedPosition > position && cappedPosition - position < chunkSize -> {
        // try and skip forwards if its a small gap
        currentSource?.source?.skip(cappedPosition - position)
      }
      else -> {
        currentSource?.close()
        currentSource = null
      }
    }

    position = cappedPosition
  }

  private data class CurrentSourceInfo(
    val bevyIndex: Int,
    val source: BufferedSource,
  ) : Closeable by source

  interface Loader : Aff4Stream.Loader<ImageStream, Aff4ImageStream>
}
