package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.io.concatLazily
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.meta.rdf.model.Hash
import com.github.nava2.aff4.meta.rdf.model.ImageStream
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.streams.Aff4Stream
import com.github.nava2.aff4.streams.Hashing.computeLinearHashes
import com.github.nava2.aff4.streams.Hashing.hashingSink
import com.github.nava2.aff4.streams.SourceProviderWithRefCounts
import com.github.nava2.aff4.streams.VerifiableStream
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Source
import okio.buffer
import java.io.Closeable

class Aff4ImageStream @AssistedInject internal constructor(
  aff4ImageBeviesFactory: Aff4ImageBevies.Factory,
  @ForImageRoot private val imageFileSystem: FileSystem,
  @Assisted private val imageStreamConfig: ImageStream,
) : VerifiableStream, Aff4Stream {

  private val sourceProviderWithRefCounts = SourceProviderWithRefCounts(::readAt)

  private val aff4ImageBevies = aff4ImageBeviesFactory.create(imageStreamConfig)
  private val chunkSize = imageStreamConfig.chunkSize
  private val bevyMaxSize = imageStreamConfig.bevyMaxSize
  private val bevyCount = imageStreamConfig.bevyCount

  val size: Long = imageStreamConfig.size

  private var position: Long = 0L

  private var currentSource: CurrentSourceInfo? = null

  private var verificationResult: VerifiableStream.Result? = null

  override fun source(position: Long): Source = sourceProviderWithRefCounts.source(position)

  override fun verify(aff4Model: Aff4Model): VerifiableStream.Result {
    if (verificationResult != null) {
      return verificationResult!!
    }

    val failedHashes = mutableListOf<Pair<String, Hash>>()

    verifyLinearHashes(failedHashes)
    verifyBlockHashes(aff4Model, failedHashes)

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

    val nextBevyIndex = position.floorDiv(bevyMaxSize).toInt()
    val maxBytesToRead = byteCount.coerceAtMost(size - position)

    val readSource = getAndUpdateCurrentSourceIfChanged(nextBevyIndex)

    val bytesRead = readSource.read(sink, maxBytesToRead)
    check(bytesRead >= 0) {
      // because of how we read these bevies by capping their read sizes, we should never read them when they
      // are exhausted but do a full read to their end point.
      "Read too much of bevy [$nextBevyIndex] - $imageStreamConfig"
    }

    position += bytesRead.coerceAtLeast(0)

    return bytesRead
  }

  private fun getAndUpdateCurrentSourceIfChanged(nextBevyIndex: Int): BufferedSource {
    val currentSource = currentSource

    if (currentSource?.bevyIndex == nextBevyIndex) {
      return currentSource.source
    }

    currentSource?.close()
    this.currentSource = null

    val bevyPosition = position % bevyMaxSize

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

  private fun verifyLinearHashes(failedHashes: MutableList<Pair<String, Hash>>) {
    val calculatedLinearHashes = source(0).buffer().use { s ->
      s.computeLinearHashes(imageStreamConfig.linearHashes.map { it.hashType })
    }

    val linearHashesByHashType = imageStreamConfig.linearHashes.associateBy { it.hashType }
    for ((hashType, actualHash) in calculatedLinearHashes) {
      val expectedHash = linearHashesByHashType.getValue(hashType)
      if (expectedHash.value != actualHash) {
        failedHashes += "Linear $hashType" to expectedHash
      }
    }
  }

  private fun verifyBlockHashes(
    aff4Model: Aff4Model,
    failedHashes: MutableList<Pair<String, Hash>>
  ) {
    val blockHashes = imageStreamConfig.queryBlockHashes(aff4Model)
    val blockHashSources = (0 until bevyCount).asSequence().map { aff4ImageBevies.getOrLoadBevy(it).bevy }
      .fold(blockHashes.associateWith { mutableListOf<() -> Source>() }) { acc, bevy ->
        for ((hash, blockHashPath) in bevy.blockHashes) {
          val sources = acc.entries.single { it.key.forHashType == hash }.value
          sources += { imageFileSystem.source(blockHashPath) }
        }

        acc
      }

    for ((blockHash, sources) in blockHashSources) {
      val actualHash = concatLazily(sources).buffer().use { source ->
        val hashSink = blockHash.hash.hashType.hashingSink()
        source.readAll(hashSink)
        hashSink.hash
      }

      if (blockHash.hash.value != actualHash) {
        failedHashes += "BlochHash ${blockHash.forHashType}" to blockHash.hash
      }
    }
  }

  private data class CurrentSourceInfo(
    val bevyIndex: Int,
    val source: BufferedSource,
  ) : Closeable by source

  interface Loader : Aff4Stream.Loader<ImageStream, Aff4ImageStream>
}
