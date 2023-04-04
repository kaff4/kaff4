package net.navatwo.kaff4.rdf

import org.eclipse.rdf4j.sail.Sail

interface RdfRepositoryConfiguration {
  fun provideStore(): Sail
}
