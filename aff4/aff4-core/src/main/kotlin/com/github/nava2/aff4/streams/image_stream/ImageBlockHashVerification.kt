package com.github.nava2.aff4.streams.image_stream

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.nava2.aff4.io.source
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.streams.Hashing.computeLinearHashes
import okio.ByteString
import okio.FileSystem
import org.eclipse.rdf4j.model.IRI
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

private const val BLOCK_HASH_VALUES_CACHE = 1024L

@Singleton
internal class ImageBlockHashVerification @Inject constructor(
  @ForImageRoot private val fileSystem: FileSystem,
) {
  private val expectedHashesCache = Caffeine.newBuilder()
    .maximumSize(BLOCK_HASH_VALUES_CACHE)
    .build<CacheKey, ExpectedHashes>()

  fun verifyBlock(bevy: Bevy, chunkIndex: Long, chunkBuffer: ByteBuffer) {
    val expectedHashes = expectedHashesCache
      .get(CacheKey(bevy.arn, chunkIndex)) {
        val byHashType = bevy.blockHashes.mapValues { (hashType, file) ->
          fileSystem.read(file) {
            skip(chunkIndex * hashType.byteCount)
            readByteString(hashType.byteCount)
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

    val actualHashes = chunkBuffer.source().use {
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
      "${bevy.arn}@$chunkIndex buff failed hashing: $verificationErrors"
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
