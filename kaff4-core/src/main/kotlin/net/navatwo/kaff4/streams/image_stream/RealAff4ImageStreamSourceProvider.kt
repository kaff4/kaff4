package net.navatwo.kaff4.streams.image_stream

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import net.navatwo.kaff4.container.ContainerDataFileSystemProvider
import net.navatwo.kaff4.io.SourceProvider
import net.navatwo.kaff4.io.buffer
import net.navatwo.kaff4.io.concatLazily
import net.navatwo.kaff4.io.sourceProvider
import net.navatwo.kaff4.io.use
import net.navatwo.kaff4.model.Aff4Model
import net.navatwo.kaff4.model.VerifiableStreamProvider
import net.navatwo.kaff4.model.VerifiableStreamProvider.Result.FailedHash
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.ImageStream
import net.navatwo.kaff4.streams.computeLinearHashes
import net.navatwo.kaff4.streams.hashingSink
import okio.Source
import okio.Timeout

internal class RealAff4ImageStreamSourceProvider @AssistedInject constructor(
  aff4ImageBeviesFactory: Aff4ImageBevies.Factory,
  private val containerDataFileSystemProvider: ContainerDataFileSystemProvider,
  @Assisted override val imageStream: ImageStream,
) : Aff4ImageStreamSourceProvider {

  override val arn: Aff4Arn = imageStream.arn
  override val size: Long = imageStream.size

  private val aff4ImageBevies: Aff4ImageBevies = aff4ImageBeviesFactory.create(imageStream)
  private val bevyCount = imageStream.bevyCount

  @Volatile
  private var verificationResult: VerifiableStreamProvider.Result? = null

  @Volatile
  private var closed: Boolean = false

  override fun source(position: Long, timeout: Timeout): Source {
    check(!closed) { "closed" }
    return Aff4ImageStreamSource(
      aff4ImageBevies = aff4ImageBevies,
      imageStream = imageStream,
      position = position,
      timeout = timeout,
    )
  }

  override fun close() {
    if (closed) return
    closed = true

    aff4ImageBevies.close()
  }

  override fun verify(aff4Model: Aff4Model, timeout: Timeout): VerifiableStreamProvider.Result {
    check(!closed) { "closed" }

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

  override fun toString(): String {
    return "Aff4ImageStream(${imageStream.arn}, closed=$closed)"
  }

  private fun verifyLinearHashes(timeout: Timeout): Sequence<FailedHash> = sequence {
    val calculatedLinearHashes = use(timeout = timeout) { s ->
      s.computeLinearHashes(imageStream.linearHashes.map { it.hashType })
    }

    val linearHashesByHashType = imageStream.linearHashes.associateBy { it.hashType }
    for ((hashType, actualHash) in calculatedLinearHashes) {
      val expectedHash = linearHashesByHashType.getValue(hashType)
      if (expectedHash.value != actualHash) {
        yield(FailedHash(imageStream, "Linear", expectedHash))
      }
    }
  }

  private fun verifyBlockHashes(aff4Model: Aff4Model, timeout: Timeout): Sequence<FailedHash> = sequence {
    val blockHashes = imageStream.queryBlockHashes(aff4Model)

    val blockHashSourceProviders = (0 until bevyCount).asSequence()
      .map { aff4ImageBevies.getOrLoadBevy(it).bevy }
      .fold(blockHashes.associateWith { mutableListOf<SourceProvider<Source>>() }) { acc, bevy ->
        for ((hash, blockHashPath) in bevy.blockHashes) {
          val sources = acc.entries.single { it.key.forHashType == hash }.value
          sources += containerDataFileSystemProvider.get(imageStream).sourceProvider(blockHashPath)
        }

        acc
      }

    for ((blockHash, sourceProviders) in blockHashSourceProviders) {
      val actualHash = sourceProviders.concatLazily().buffer().use(timeout = timeout) { source ->
        blockHash.hash.hashType.hashingSink().use { hashSink ->
          source.readAll(hashSink)
          hashSink.hash
        }
      }

      if (blockHash.hash.value != actualHash) {
        yield(FailedHash(imageStream, "BlockHash ${blockHash.forHashType}", blockHash.hash))
      }
    }
  }
}
