package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.meta.rdf.model.MapStream
import com.github.nava2.aff4.streams.AbstractAff4StreamModule
import com.github.nava2.guice.key
import com.github.nava2.guice.typeLiteral
import com.google.inject.assistedinject.FactoryModuleBuilder

internal object Aff4MapStreamModule : AbstractAff4StreamModule<MapStream, Aff4MapStream>(
  configTypeLiteral = typeLiteral(),
  loaderKey = typeLiteral<Aff4MapStream.Loader>().key,
) {
  override fun configureModule() {
    install(FactoryModuleBuilder().build(Aff4MapStream.Loader::class.java))
  }
}
