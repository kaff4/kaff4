package com.github.nava2.aff4.meta.rdf

import com.github.nava2.aff4.model.rdf.Aff4Schema
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory

fun ValueFactory.createAff4Iri(aff4LocalName: String): IRI {
  return createIRI(Aff4Schema.SCHEMA, aff4LocalName)
}
