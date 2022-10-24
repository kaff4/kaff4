package com.github.nava2.aff4.rdf

import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.key
import com.github.nava2.guice.to
import com.github.nava2.guice.typeLiteral
import com.google.inject.Provides
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import javax.inject.Singleton

object RdfRepositoryModule : KAbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    requireBinding(typeLiteral<RdfRepositoryConfiguration>().key)

    bind<RdfExecutor>().to<RealRdfExecutor>()
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
