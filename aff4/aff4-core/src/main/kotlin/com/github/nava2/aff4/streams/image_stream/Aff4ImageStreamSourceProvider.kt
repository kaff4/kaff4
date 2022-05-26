package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.io.SourceProvider
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
import com.github.nava2.aff4.streams.computeLinearHashes
import com.github.nava2.aff4.streams.hashingSink
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.FileSystem
import okio.Source
import okio.Timeout

class Aff4ImageStreamSourceProvider @AssistedInject internal constructor(
  aff4ImageBeviesFactory: Aff4ImageBevies.Factory,
  @ForImageRoot private val imageFileSystem: FileSystem,
  @Assisted private val imageStreamConfig: ImageStream,
) : VerifiableStreamProvider, Aff4StreamSourceProvider {

  private val aff4ImageBevies: Aff4ImageBevies = aff4ImageBeviesFactory.create(imageStreamConfig)
  private val bevyCount = imageStreamConfig.bevyCount

  override val size: Long = imageStreamConfig.size

  @Volatile
  private var verificationResult: VerifiableStreamProvider.Result? = null

  override fun source(position: Long, timeout: Timeout): Source = Aff4ImageStreamSource(
    aff4ImageBevies = aff4ImageBevies,
    imageStream = imageStreamConfig,
    position = position,
    timeout = timeout,
  )

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

  override fun close() = Unit

  override fun toString(): String {
    return "Aff4ImageStream(${imageStreamConfig.arn})"
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

  interface Loader : Aff4StreamSourceProvider.Loader<ImageStream, Aff4ImageStreamSourceProvider>
}
