package com.github.nava2.aff4.streams.zip_segment

import com.github.nava2.aff4.container.ContainerDataFileSystemProvider
import com.github.nava2.aff4.io.SourceProvider
import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.sourceProvider
import com.github.nava2.aff4.io.use
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.VerifiableStreamProvider
import com.github.nava2.aff4.model.VerifiableStreamProvider.Result.FailedHash
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.aff4.streams.computeLinearHashes
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.Source
import okio.Timeout

internal class RealAff4ZipSegmentSourceProvider @AssistedInject constructor(
  private val containerDataFileSystemProvider: ContainerDataFileSystemProvider,
  @Assisted override val zipSegment: ZipSegment,
) : Aff4ZipSegmentSourceProvider,
  SourceProvider<Source> by containerDataFileSystemProvider.get(zipSegment)
    .sourceProvider(zipSegment.segmentPath)
    .buffer() {

  override val arn: Aff4Arn = zipSegment.arn
  override val size = zipSegment.size

  @Volatile
  private var verificationResult: VerifiableStreamProvider.Result? = null

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
}