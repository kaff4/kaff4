package com.github.nava2.aff4.model

import com.github.nava2.aff4.streams.Aff4StreamLoaderContext
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.to

internal object Aff4StreamOpenerModule : KAbstractModule() {
  override fun configure() {
    bind<Aff4StreamOpener>().to<RealAff4StreamOpener>()

    bindSet<Aff4StreamLoaderContext> { }
  }
}
