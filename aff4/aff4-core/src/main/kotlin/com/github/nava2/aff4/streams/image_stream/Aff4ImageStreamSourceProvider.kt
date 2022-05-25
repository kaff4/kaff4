package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.io.SourceProvider
import com.github.nava2.aff4.io.applyAndCloseOnThrow
import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.concatLazily
import com.github.nava2.aff4.io.sourceProvider
import com.github.nava2.aff4.io.use
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4StreamSourceProvider
import com.github.nava2.aff4.model.VerifiableStreamProvider
import com.github.nava2.aff4.model.VerifiableStreamProvider.Result.FailedHash
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.streams.SourceProviderWithRefCounts
import com.github.nava2.aff4.streams.computeLinearHashes
import com.github.nava2.aff4.streams.hashingSink
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Source
import okio.Timeout
import java.io.Closeable

class Aff4ImageStreamSourceProvider @AssistedInject internal constructor(
  aff4ImageBeviesFactory: Aff4ImageBevies.Factory,
  @ForImageRoot private val imageFileSystem: FileSystem,
  @Assisted private val imageStreamConfig: ImageStream,
) : VerifiableStreamProvider, Aff4StreamSourceProvider, SourceProviderWithRefCounts.SourceDelegate {

  private val sourceProviderWithRefCounts = SourceProviderWithRefCounts(this)

  private val aff4ImageBevies = aff4ImageBeviesFactory.create(imageStreamConfig)
  private val chunkSize = imageStreamConfig.chunkSize
  private val bevyMaxSize = imageStreamConfig.bevyMaxSize
  private val bevyCount = imageStreamConfig.bevyCount

  override val size: Long = imageStreamConfig.size

  private var position: Long = 0L

  private var currentSource: CurrentSourceInfo? = null

  @Volatile
  private var verificationResult: VerifiableStreamProvider.Result? = null

  override fun source(position: Long, timeout: Timeout): Source = sourceProviderWithRefCounts.source(position, timeout)

  override fun verify(aff4Model: Aff4Model, timeout: Timeout): VerifiableStreamProvider.Result {
    val previousResult = verificationResult
    if (previousResult != null) {
      return previousResult
    }

    return synchronized(this) {
      val doubleChecked = verificationResult
      if (doubleChecked != null) return doubleChecked

      timeout.throwIfReached()

      val failedHashes = mutableListOf<FailedHash>()
      failedHashes += verifyLinearHashes(timeout)
      failedHashes += verifyBlockHashes(aff4Model, timeout)

      val result = VerifiableStreamProvider.Result.fromFailedHashes(failedHashes)
      verificationResult = result
      result
    }
  }

  override fun close() {
    currentSource?.close()
    currentSource = null

    sourceProviderWithRefCounts.close()
    aff4ImageBevies.close()
  }

  override fun toString(): String {
    return "Aff4ImageStream(${imageStreamConfig.arn})"
  }

  override fun readAt(readPosition: Long, timeout: Timeout, sink: Buffer, byteCount: Long): Long {
    moveTo(readPosition)

    // we are exhausted
    if (position == size) return -1L

    val nextBevyIndex = position.floorDiv(bevyMaxSize).toInt()
    val maxBytesToRead = byteCount.coerceAtMost(size - position)

    val readSource = getAndUpdateCurrentSourceIfChanged(nextBevyIndex, timeout)

    val bytesRead = readSource.read(sink, maxBytesToRead)
    check(bytesRead >= 0) {
      // because of how we read these bevies by capping their read sizes, we should never read them when they
      // are exhausted but do a full read to their end point.
      "Read too much of bevy [$nextBevyIndex] - $imageStreamConfig"
    }

    position += bytesRead

    return bytesRead
  }

  private fun getAndUpdateCurrentSourceIfChanged(nextBevyIndex: Int, timeout: Timeout): BufferedSource {
    val currentSource = currentSource

    if (currentSource?.bevyIndex == nextBevyIndex) {
      return currentSource.source
    }

    currentSource?.close()
    this.currentSource = null

    val bevyPosition = position % bevyMaxSize

    return aff4ImageBevies.getOrLoadBevy(nextBevyIndex).buffer().source(bevyPosition, timeout).applyAndCloseOnThrow {
      val sourceInfo = CurrentSourceInfo(nextBevyIndex, this)

      this@Aff4ImageStreamSourceProvider.currentSource = sourceInfo
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

  private fun verifyLinearHashes(timeout: Timeout): Sequence<FailedHash> = sequence {
    val calculatedLinearHashes = use(timeout = timeout) { s ->
      s.computeLinearHashes(imageStreamConfig.linearHashes.map { it.hashType })
    }

    val linearHashesByHashType = imageStreamConfig.linearHashes.associateBy { it.hashType }
    for ((hashType, actualHash) in calculatedLinearHashes) {
      val expectedHash = linearHashesByHashType.getValue(hashType)
      if (expectedHash.value != actualHash) {
        yield(FailedHash(imageStreamConfig, "Linear", expectedHash))
      }
    }
  }

  private fun verifyBlockHashes(aff4Model: Aff4Model, timeout: Timeout): Sequence<FailedHash> = sequence {
    val blockHashes = imageStreamConfig.queryBlockHashes(aff4Model)

    val blockHashSourceProviders = (0 until bevyCount).asSequence()
      .map { aff4ImageBevies.getOrLoadBevy(it).bevy }
      .fold(blockHashes.associateWith { mutableListOf<SourceProvider<Source>>() }) { acc, bevy ->
        for ((hash, blockHashPath) in bevy.blockHashes) {
          val sources = acc.entries.single { it.key.forHashType == hash }.value
          sources += imageFileSystem.sourceProvider(blockHashPath)
        }

        acc
      }

    for ((blockHash, sourceProviders) in blockHashSourceProviders) {
      val actualHash = concatLazily(sourceProviders).buffer().use(timeout = timeout) { source ->
        blockHash.hash.hashType.hashingSink().use { hashSink ->
          source.readAll(hashSink)
          hashSink.hash
        }
      }

      if (blockHash.hash.value != actualHash) {
        yield(FailedHash(imageStreamConfig, "BlockHash ${blockHash.forHashType}", blockHash.hash))
      }
    }
  }

  private data class CurrentSourceInfo(
    val bevyIndex: Int,
    val source: BufferedSource,
  ) : Closeable by source

  interface Loader : Aff4StreamSourceProvider.Loader<ImageStream, Aff4ImageStreamSourceProvider>
}
