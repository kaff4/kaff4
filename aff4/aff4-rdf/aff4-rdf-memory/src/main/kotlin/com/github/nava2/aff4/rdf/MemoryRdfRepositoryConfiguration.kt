package com.github.nava2.aff4.rdf

import org.eclipse.rdf4j.sail.Sail
import org.eclipse.rdf4j.sail.memory.MemoryStore
import javax.inject.Singleton

@Singleton
class MemoryRdfRepositoryConfiguration : RdfRepositoryConfiguration {
  private val memoryStore by lazy { MemoryStore() }

  override fun provideStore(): Sail = memoryStore
}
