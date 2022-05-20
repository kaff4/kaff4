package com.github.nava2.aff4.rdf

object MemoryRdfRepositoryConfiguration : RdfRepositoryConfiguration {
  private val memoryStore by lazy { org.eclipse.rdf4j.sail.memory.MemoryStore() }

  override fun provideStore(): org.eclipse.rdf4j.sail.Sail = memoryStore
}
