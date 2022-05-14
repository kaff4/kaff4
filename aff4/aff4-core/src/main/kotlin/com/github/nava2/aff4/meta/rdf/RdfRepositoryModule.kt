package com.github.nava2.aff4.meta.rdf

import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.key
import com.github.nava2.guice.typeLiteral
import com.google.inject.Provides
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import javax.inject.Singleton

object RdfRepositoryModule : KAbstractModule() {
  override fun configure() {
    requireBinding(typeLiteral<RdfRepositoryConfiguration>().key)

    install(RdfConnectionScopeModule)
  }

  @Provides
  @Singleton
  fun providesValueFactory(repository: Repository) = repository.valueFactory

  @Provides
  @Singleton
  fun providesRepository(rdfRepositoryConfiguration: RdfRepositoryConfiguration): Repository {
    return SailRepository(rdfRepositoryConfiguration.provideStore())
  }
}

