package com.github.nava2.aff4.rdf

import com.github.nava2.guice.KAff4AbstractModule
import com.github.nava2.guice.assistedFactoryModule
import com.github.nava2.guice.key
import com.github.nava2.guice.to
import com.google.inject.Provides
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import javax.inject.Singleton

object RdfRepositoryModule : KAff4AbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    requireBinding(key<RdfRepositoryConfiguration>())

    bind<RdfExecutor>().to<RealRdfExecutor>()

    install(assistedFactoryModule<TurtleReaderAndInserter.Factory>())
    install(assistedFactoryModule<RealMutableRdfConnection.Factory>())
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
