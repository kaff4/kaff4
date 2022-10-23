package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.model.Aff4StreamOpener
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.streams.AbstractAff4StreamModule
import com.github.nava2.guice.assistedFactoryModule
import com.github.nava2.guice.key
import com.github.nava2.guice.typeLiteral

internal object Aff4MapStreamModule : AbstractAff4StreamModule<MapStream, Aff4MapStreamSourceProvider>(
  configTypeLiteral = typeLiteral(),
  loaderKey = typeLiteral<Aff4MapStreamSourceProvider.Loader>().key,
) {
  override fun configureModule() {
    requireBinding<Aff4StreamOpener>()

    install(assistedFactoryModule<Aff4MapStreamSourceProvider.Loader>())
  }
}
