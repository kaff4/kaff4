package com.github.nava2.aff4.model

import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.model.rdf.Hash
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
      val hash: Hash,
    )

    companion object {
      fun fromFailedHashes(failureReasons: Collection<FailedHash>): Result = if (failureReasons.isNotEmpty()) {
        Failed(failureReasons)
      } else {
        Success
      }
    }
  }
}
