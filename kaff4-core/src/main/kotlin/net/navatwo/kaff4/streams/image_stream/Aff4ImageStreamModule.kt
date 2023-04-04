package net.navatwo.kaff4.streams.image_stream

import com.google.inject.assistedinject.FactoryModuleBuilder
import net.navatwo.guice.assistedFactoryModule
import net.navatwo.guice.build
import net.navatwo.guice.implement
import net.navatwo.guice.key
import net.navatwo.guice.typeLiteral
import net.navatwo.kaff4.model.rdf.ImageStream
import net.navatwo.kaff4.streams.AbstractAff4StreamModule

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
