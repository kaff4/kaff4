package com.github.nava2.aff4.streams.image_stream

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.nava2.aff4.io.source
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.meta.rdf.model.HashType
import com.github.nava2.aff4.streams.Hashing.computeLinearHashes
import okio.ByteString
import okio.FileSystem
import okio.buffer
import org.eclipse.rdf4j.model.IRI
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ImageBlockHashVerification @Inject constructor(
  @ForImageRoot private val fileSystem: FileSystem,
) {
  private val verificationCache = Caffeine.newBuilder()
    .maximumSize(1024)
    .build<CacheKey, VerificationErrors>()

  private data class CacheKey(
    val bevyArn: IRI,
    val chunkIndex: Long,
  )

  private data class VerificationErrors(
    val errors: List<Pair<HashType, ByteString>>,
  ) {
    companion object {
      val EMPTY = VerificationErrors(listOf())
    }
  }

  fun verifyBlock(bevy: Bevy, chunkIndex: Long, chunkBuffer: ByteBuffer) {
    val verificationErrors = verificationCache.get(CacheKey(bevy.arn, chunkIndex)) {
      val expectedHashes = bevy.blockHashes.mapValues { (hashType, file) ->
        fileSystem.read(file) {
          skip(chunkIndex * hashType.byteCount)
          readByteString(hashType.byteCount)
        }
      }

      chunkBuffer.mark()

      chunkBuffer.position(0)

      val actualHashes = chunkBuffer.source().buffer().use {
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

      if (verificationErrors.isNotEmpty()) {
        VerificationErrors(verificationErrors)
      } else {
        VerificationErrors.EMPTY
      }
    }

    check(verificationErrors.errors.isEmpty()) {
      "${bevy.arn}@$chunkIndex buff failed hashing: ${verificationErrors.errors}"
    }
  }
}