package com.github.nava2.aff4.streams

import com.github.nava2.aff4.meta.rdf.model.Aff4RdfModel
import com.github.nava2.guice.KAbstractModule
import com.google.inject.Key
import com.google.inject.TypeLiteral

internal abstract class AbstractAff4StreamModule<C : Aff4RdfModel, S : Aff4Stream>(
  private val configTypeLiteral: TypeLiteral<C>,
  private val loaderKey: Key<out Aff4Stream.Loader<C, S>>,
) : KAbstractModule() {
  open fun configureModule() = Unit

  final override fun configure() {
    configureModule()

    bindSet<Aff4StreamLoaderContext> {
      toInstance(Aff4StreamLoaderContext(configTypeLiteral, binder().getProvider(loaderKey)))
    }
  }
}
