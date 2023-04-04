package com.github.nava2.aff4.rdf

import org.eclipse.rdf4j.sail.Sail

interface RdfRepositoryConfiguration {
  fun provideStore(): Sail
}
