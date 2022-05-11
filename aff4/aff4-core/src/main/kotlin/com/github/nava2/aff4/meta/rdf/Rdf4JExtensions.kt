package com.github.nava2.aff4.meta.rdf

import org.eclipse.rdf4j.common.iteration.Iteration

fun <E : Any, X : Exception> Iteration<E, X>.asSequence(): Sequence<E> = sequence {
  while (hasNext()) {
    yield(next())
  }
}
