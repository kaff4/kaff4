package com.github.nava2.aff4.streams

import com.github.nava2.aff4.model.Aff4StreamSourceProvider
import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.guice.KAbstractModule
import com.google.inject.Key
import com.google.inject.TypeLiteral

internal abstract class AbstractAff4StreamModule<C : Aff4RdfModel, S : Aff4StreamSourceProvider>(
  private val configTypeLiteral: TypeLiteral<C>,
  private val loaderKey: Key<out Aff4StreamSourceProvider.Loader<C, S>>,
) : KAbstractModule() {
  open fun configureModule() = Unit

  final override fun configure() {
    configureModule()

    bindSet<Aff4StreamLoaderContext> {
      toInstance(Aff4StreamLoaderContext(configTypeLiteral, binder().getProvider(loaderKey)))
    }
  }
}
