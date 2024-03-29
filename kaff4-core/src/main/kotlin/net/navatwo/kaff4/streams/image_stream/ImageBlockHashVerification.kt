package net.navatwo.kaff4.streams.image_stream

import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.navatwo.kaff4.container.ContainerDataFileSystemProvider
import net.navatwo.kaff4.io.source
import net.navatwo.kaff4.model.rdf.HashType
import net.navatwo.kaff4.streams.computeLinearHashes
import okio.ByteString
import okio.Timeout
import org.eclipse.rdf4j.model.IRI
import java.nio.ByteBuffer

private const val BLOCK_HASH_VALUES_CACHE = 1024L

@Singleton
internal class ImageBlockHashVerification @Inject constructor(
  private val containerDataFileSystemProvider: ContainerDataFileSystemProvider,
) {
  private val expectedHashesCache = Caffeine.newBuilder()
    .maximumSize(BLOCK_HASH_VALUES_CACHE)
    .build<CacheKey, ExpectedHashes>()

  fun verifyBlock(bevy: Bevy, chunkIndex: Long, chunkBuffer: ByteBuffer, timeout: Timeout) {
    val expectedHashes = expectedHashesCache
      .get(CacheKey(bevy.arn, chunkIndex)) {
        timeout.throwIfReached()

        val fileSystem by containerDataFileSystemProvider.lazy(bevy.stored)

        val byHashType = bevy.blockHashes.mapValues { (hashType, file) ->
          timeout.throwIfReached()

          fileSystem.read(file) {
            skip(chunkIndex * hashType.byteCount)
            readByteString(hashType.byteCount.toLong())
          }
        }
        if (byHashType.isEmpty()) {
          ExpectedHashes.EMPTY
        } else {
          ExpectedHashes(byHashType)
        }
      }
      .byHashType

    chunkBuffer.mark()

    chunkBuffer.position(0)

    val actualHashes = chunkBuffer.source(timeout).use {
      it.computeLinearHashes(expectedHashes.keys)
    }

    chunkBuffer.reset()

    val verificationErrors = mutableListOf<Pair<HashType, ByteString>>()
    for (hashType in expectedHashes.keys) {
      val expectedHash = expectedHashes.getValue(hashType)
      val actualHash = actualHashes.getValue(hashType)

      if (expectedHash != actualHash) {
        verificationErrors += hashType to expectedHash
      }
    }

    check(verificationErrors.isEmpty()) {
      "${bevy.arn}@$chunkIndex failed block-hash verification: $verificationErrors"
    }
  }

  private data class CacheKey(
    val bevyArn: IRI,
    val chunkIndex: Long,
  )

  private data class ExpectedHashes(
    val byHashType: Map<HashType, ByteString>,
  ) {
    companion object {
      val EMPTY = ExpectedHashes(mapOf())
    }
  }
}
