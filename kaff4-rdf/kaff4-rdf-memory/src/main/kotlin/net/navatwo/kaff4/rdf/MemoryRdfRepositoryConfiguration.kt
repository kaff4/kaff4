package net.navatwo.kaff4.rdf

import org.eclipse.rdf4j.sail.Sail
import org.eclipse.rdf4j.sail.memory.MemoryStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MemoryRdfRepositoryConfiguration @Inject constructor() : RdfRepositoryConfiguration {
  private val memoryStore by lazy { MemoryStore() }

  override fun provideStore(): Sail = memoryStore
}
