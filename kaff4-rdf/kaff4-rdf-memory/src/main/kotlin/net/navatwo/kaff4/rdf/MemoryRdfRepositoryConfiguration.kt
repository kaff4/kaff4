package net.navatwo.kaff4.rdf

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.eclipse.rdf4j.sail.Sail
import org.eclipse.rdf4j.sail.memory.MemoryStore

@Singleton
internal class MemoryRdfRepositoryConfiguration @Inject constructor() : RdfRepositoryConfiguration {
  private val memoryStore by lazy { MemoryStore() }

  override fun provideStore(): Sail = memoryStore
}
