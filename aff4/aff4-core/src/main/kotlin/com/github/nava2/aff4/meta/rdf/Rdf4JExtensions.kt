package com.github.nava2.aff4.meta.rdf

import org.eclipse.rdf4j.common.iteration.Iteration
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory

fun <E : Any, X : Exception> Iteration<E, X>.asSequence(): Sequence<E> = sequence {
  while (hasNext()) {
    yield(next())
  }
}

fun ValueFactory.createAff4Iri(aff4LocalName: String): IRI {
  return createIRI(Aff4Schema.SCHEMA, aff4LocalName)
}
