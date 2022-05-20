package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.streams.AbstractAff4StreamModule
import com.github.nava2.guice.key
import com.github.nava2.guice.typeLiteral
import com.google.inject.assistedinject.FactoryModuleBuilder

internal object Aff4ImageStreamModule : AbstractAff4StreamModule<ImageStream, Aff4ImageStream>(
  configTypeLiteral = typeLiteral(),
  loaderKey = typeLiteral<Aff4ImageStream.Loader>().key,
) {
  override fun configureModule() {
    install(FactoryModuleBuilder().build(Aff4ImageStream.Loader::class.java))

    install(FactoryModuleBuilder().build(BevyIndexReader.Factory::class.java))
    install(FactoryModuleBuilder().build(Aff4ImageBevies.Factory::class.java))
    install(FactoryModuleBuilder().build(Aff4Bevy.AssistedFactory::class.java))
  }
}
