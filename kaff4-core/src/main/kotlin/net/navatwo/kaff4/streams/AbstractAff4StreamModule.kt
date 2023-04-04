package net.navatwo.kaff4.streams

import com.google.inject.Key
import com.google.inject.TypeLiteral
import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.kaff4.model.Aff4StreamSourceProvider
import net.navatwo.kaff4.model.rdf.Aff4RdfModel

internal abstract class AbstractAff4StreamModule<C : Aff4RdfModel, S : Aff4StreamSourceProvider>(
  private val configTypeLiteral: TypeLiteral<C>,
  private val loaderKey: Key<out Aff4StreamSourceProvider.Loader<C, S>>,
) : KAff4AbstractModule() {
  open fun configureModule() = Unit

  final override fun configure() {
    configureModule()

    bindSet<Aff4StreamLoaderContext> {
      toInstance(Aff4StreamLoaderContext(configTypeLiteral, binder().getProvider(loaderKey)))
    }
  }
}
