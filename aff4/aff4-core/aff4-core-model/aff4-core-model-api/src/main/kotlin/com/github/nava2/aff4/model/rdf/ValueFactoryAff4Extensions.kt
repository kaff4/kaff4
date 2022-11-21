package com.github.nava2.aff4.model.rdf

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory

fun ValueFactory.createAff4Iri(aff4LocalName: String): IRI {
  return createIRI(Aff4Schema.SCHEMA, aff4LocalName)
}
