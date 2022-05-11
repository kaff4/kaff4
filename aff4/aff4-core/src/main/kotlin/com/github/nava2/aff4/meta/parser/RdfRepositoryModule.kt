package com.github.nava2.aff4.meta.parser

import com.github.nava2.aff4.meta.rdf.RdfConnectionScopeModule
import com.github.nava2.guice.KAbstractModule
import com.google.inject.Provides
import com.google.inject.util.Modules
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import javax.inject.Singleton

object RdfRepositoryModule : KAbstractModule() {
  override fun configure() {
    bind<Repository>().toInstance(SailRepository(MemoryStore()))

    install(RdfConnectionScopeModule)
  }

  @Provides
  @Singleton
  fun providesValueFactory(repository: Repository) = repository.valueFactory
}

object TestRdfRepositoryModule : com.google.inject.Module by Modules.override(RdfRepositoryModule).with(
  object : KAbstractModule() {
    override fun configure() {
      bind<Repository>().toInstance(SailRepository(MemoryStore()))
    }
  }
)
