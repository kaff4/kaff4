package net.navatwo.kaff4.model

import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.guice.to
import net.navatwo.kaff4.streams.Aff4StreamLoaderContext

internal object Aff4StreamOpenerModule : KAff4AbstractModule() {
  override fun configure() {
    bind<Aff4StreamOpener>().to<RealAff4StreamOpener>()

    bindSet<Aff4StreamLoaderContext> { }
  }
}
