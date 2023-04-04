package net.navatwo.kaff4.streams.map_stream

import com.google.inject.assistedinject.FactoryModuleBuilder
import net.navatwo.guice.build
import net.navatwo.guice.implement
import net.navatwo.guice.key
import net.navatwo.guice.typeLiteral
import net.navatwo.kaff4.model.Aff4StreamOpener
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.streams.AbstractAff4StreamModule

internal object Aff4MapStreamModule : AbstractAff4StreamModule<MapStream, Aff4MapStreamSourceProvider>(
  configTypeLiteral = typeLiteral(),
  loaderKey = typeLiteral<Aff4MapStreamSourceProvider.Loader>().key,
) {
  override fun configureModule() {
    requireBinding<Aff4StreamOpener>()

    install(
      FactoryModuleBuilder()
        .implement<Aff4MapStreamSourceProvider, RealAff4MapStreamSourceProvider>()
        .build<Aff4MapStreamSourceProvider.Loader>()
    )
  }
}
