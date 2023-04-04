package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.streams.AbstractAff4StreamModule
import com.github.nava2.guice.assistedFactoryModule
import com.github.nava2.guice.build
import com.github.nava2.guice.implement
import com.github.nava2.guice.key
import com.github.nava2.guice.typeLiteral
import com.google.inject.assistedinject.FactoryModuleBuilder

internal object Aff4ImageStreamModule : AbstractAff4StreamModule<ImageStream, Aff4ImageStreamSourceProvider>(
  configTypeLiteral = typeLiteral(),
  loaderKey = typeLiteral<Aff4ImageStreamSourceProvider.Loader>().key,
) {
  override fun configureModule() {
    install(
      FactoryModuleBuilder()
        .implement<Aff4ImageStreamSourceProvider, RealAff4ImageStreamSourceProvider>()
        .build<Aff4ImageStreamSourceProvider.Loader>()
    )

    install(assistedFactoryModule<BevyIndexReader.Factory>())
    install(assistedFactoryModule<Aff4ImageBevies.Factory>())
    install(assistedFactoryModule<Aff4BevySourceProvider.AssistedFactory>())
  }
}
