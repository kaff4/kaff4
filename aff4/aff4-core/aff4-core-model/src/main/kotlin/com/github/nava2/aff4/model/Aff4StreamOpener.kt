package com.github.nava2.aff4.model

import org.eclipse.rdf4j.model.IRI

interface Aff4StreamOpener {
  fun openStream(iri: IRI): Aff4Stream
}
