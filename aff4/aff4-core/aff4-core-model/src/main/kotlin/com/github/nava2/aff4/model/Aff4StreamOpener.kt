package com.github.nava2.aff4.model

import com.github.nava2.aff4.io.SourceProvider
import okio.Source
import org.eclipse.rdf4j.model.IRI

interface Aff4StreamOpener : AutoCloseable {
  fun openStream(iri: IRI): SourceProvider<Source>
}
