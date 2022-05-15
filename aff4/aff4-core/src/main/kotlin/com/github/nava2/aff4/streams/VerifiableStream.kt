package com.github.nava2.aff4.streams

import com.github.nava2.aff4.meta.rdf.model.Hash

interface VerifiableStream {
  /** Verifies `this` streams content. This may recompute on each call or may reuse previous results. */
  fun verify(): Result

  sealed interface Result {
    object Success : Result

    class Failed(val reasons: List<Pair<String, Hash>>) : Result
  }
}
