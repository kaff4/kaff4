package net.navatwo.kaff4.rdf

import com.google.inject.Provides
import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.guice.assistedFactoryModule
import net.navatwo.guice.key
import net.navatwo.guice.to
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
