package com.github.nava2.aff4.streams

import com.github.nava2.aff4.io.exhaust
import com.github.nava2.aff4.meta.rdf.model.Hash
import com.github.nava2.aff4.meta.rdf.model.ImageStream
import com.github.nava2.aff4.meta.rdf.parser.ForImageRoot
import com.github.nava2.aff4.streams.Hashing.computeLinearHashes
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.HashingSink
import okio.Sink
import okio.Source
import okio.blackholeSink
import okio.buffer
import java.io.Closeable

class Aff4ImageStream internal constructor(
  bevyFactory: Bevy.Factory,
  @ForImageRoot private val fileSystem: FileSystem,
  private val imageStreamConfig: ImageStream,
) : VerifiableStream, AutoCloseable {
  private val sourceProviderWithRefCounts = SourceProviderWithRefCounts(::readAt)

  private val aff4ImageBevies = Aff4ImageBevies(bevyFactory, fileSystem, imageStreamConfig)
  private val chunksInSegment = imageStreamConfig.chunksInSegment
  private val chunkSize = imageStreamConfig.chunkSize
  private val bevySize = chunksInSegment * chunkSize

  val size: Long = imageStreamConfig.size

  private var position: Long = 0L

  private var currentSource: CurrentSourceInfo? = null

  private var verificationResult: VerifiableStream.Result? = null

  fun source(position: Long): Source {
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

    if (position == size) return -1L

    // we are exhausted
    val nextBevyIndex = position.floorDiv(bevySize).toInt()
    val maxBytesToRead = byteCount.coerceAtMost(size - position)

    val readSource = getAndUpdateCurrentSourceIfChanged(nextBevyIndex)

    val bytesRead = readSource.exhaust(sink, maxBytesToRead)

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
}

internal fun Hash.hashingSink(delegateSink: Sink = blackholeSink()): HashingSink {
  return when (this) {
    is Hash.Sha1 -> HashingSink.sha1(delegateSink)
    is Hash.Md5 -> HashingSink.md5(delegateSink)
    is Hash.Sha256 -> HashingSink.sha256(delegateSink)
    is Hash.Sha512 -> HashingSink.sha512(delegateSink)
  }
}
