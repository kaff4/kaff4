package net.navatwo.kaff4.model

import net.navatwo.kaff4.model.rdf.Aff4RdfModel
import net.navatwo.kaff4.model.rdf.Hash
import okio.ByteString
import okio.Timeout

interface VerifiableStreamProvider {
  /** Verifies `this` streams content. This may recompute on each call or may reuse previous results. */
  fun verify(aff4Model: Aff4Model, timeout: Timeout = Timeout.NONE): Result

  sealed interface Result {
    val failedHashes: Collection<FailedHash>

    object Success : Result {
      override val failedHashes: Collection<FailedHash> = listOf()

      override fun toString(): String = javaClass.simpleName
    }

    data class Failed(override val failedHashes: Collection<FailedHash>) : Result

    data class FailedHash(
      val stream: Aff4RdfModel,
      val name: String,
      val expectedHash: Hash,
      val actualHash: Hash,
    ) {
      constructor(
        stream: Aff4RdfModel,
        name: String,
        expectedHash: Hash,
        actualHash: ByteString,
      ) : this(stream, name, expectedHash, expectedHash.hashType.value(actualHash))
    }

    companion object {
      fun fromFailedHashes(failureReasons: Collection<FailedHash>): Result = if (failureReasons.isNotEmpty()) {
        Failed(failureReasons)
      } else {
        Success
      }
    }
  }
}
