package com.github.nava2.aff4.streams.zip_segment

import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.use
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4StreamSourceProvider
import com.github.nava2.aff4.model.VerifiableStreamProvider
import com.github.nava2.aff4.model.VerifiableStreamProvider.Result.FailedHash
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.aff4.streams.SourceProviderWithRefCounts
import com.github.nava2.aff4.streams.computeLinearHashes
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Source
import okio.Timeout
import okio.buffer

internal class Aff4ZipSegmentSourceProvider @AssistedInject constructor(
  @ForImageRoot private val imageFileSystem: FileSystem,
  @Assisted val zipSegment: ZipSegment,
) : Aff4StreamSourceProvider, VerifiableStreamProvider, SourceProviderWithRefCounts.SourceDelegate {
  override val size = zipSegment.size

  private val sourceProviderWithRefCounts = SourceProviderWithRefCounts(this)

  private var position: Long = 0L
  private var currentSource: BufferedSource? = null

  @Volatile
  private var verificationResult: VerifiableStreamProvider.Result? = null

  override fun source(position: Long, timeout: Timeout): Source = sourceProviderWithRefCounts.source(position, timeout)

  override fun close() {
    resetCurrentSource()
    sourceProviderWithRefCounts.close()
  }

  override fun verify(aff4Model: Aff4Model, timeout: Timeout): VerifiableStreamProvider.Result {
    val previousResult = verificationResult
    if (previousResult != null) {
      return previousResult
    }

    return synchronized(this) {
      val doubleChecked = verificationResult
      if (doubleChecked != null) return doubleChecked

      val failedHashes = mutableListOf<FailedHash>()

      val actualLinearHashes = buffer().use(timeout) { source ->
        source.computeLinearHashes(zipSegment.linearHashes.map { it.hashType })
      }

      for (expectedHash in zipSegment.linearHashes) {
        val actualHash = actualLinearHashes.getValue(expectedHash.hashType)
        if (expectedHash.value != actualHash) {
          failedHashes += FailedHash(zipSegment, "Linear", expectedHash)
        }
      }

      val result = if (failedHashes.isNotEmpty()) {
        VerifiableStreamProvider.Result.Failed(failedHashes)
      } else {
        VerifiableStreamProvider.Result.Success
      }

      verificationResult = result
      result
    }
  }

  override fun readAt(readPosition: Long, timeout: Timeout, sink: Buffer, byteCount: Long): Long {
    timeout.throwIfReached()

    moveTo(readPosition)

    val maxBytesToRead = byteCount.coerceAtMost(size - readPosition)
    val readSource = getAndUpdateCurrentSourceIfChanged(readPosition)

    val bytesRead = readSource.read(sink, maxBytesToRead)
    position += bytesRead.coerceAtLeast(0)

    return bytesRead
  }

  private fun getAndUpdateCurrentSourceIfChanged(nextPosition: Long): BufferedSource {
    val currentSource = currentSource
    if (currentSource != null && position == nextPosition) return currentSource

    resetCurrentSource()

    val nextSource = imageFileSystem.source(zipSegment.segmentPath)
      .buffer()
      .apply {
        skip(nextPosition)
      }

    this.currentSource = nextSource

    return nextSource
  }

  private fun moveTo(newPosition: Long) {
    val cappedPosition = newPosition.coerceAtMost(size)
    if (cappedPosition == position) return

    val currentSource = this.currentSource

    when {
      currentSource == null -> Unit
      cappedPosition > position -> {
        // try and skip forwards if its a small gap
        currentSource.skip(cappedPosition - position)
      }
      else -> resetCurrentSource()
    }

    position = cappedPosition
  }

  private fun resetCurrentSource() {
    currentSource?.close()
    currentSource = null
  }

  interface Loader : Aff4StreamSourceProvider.Loader<ZipSegment, Aff4ZipSegmentSourceProvider>
}
