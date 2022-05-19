package com.github.nava2.aff4.streams

import com.github.nava2.aff4.meta.rdf.model.Hash
import com.github.nava2.aff4.model.Aff4Model

interface VerifiableStream {
  /** Verifies `this` streams content. This may recompute on each call or may reuse previous results. */
  fun verify(aff4Model: Aff4Model): Result

  sealed interface Result {
    object Success : Result {
      override fun toString(): String = javaClass.simpleName
    }

    data class Failed(val reasons: List<Pair<String, Hash>>) : Result
  }
}
