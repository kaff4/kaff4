package com.github.nava2.aff4.model

import com.github.nava2.aff4.streams.Aff4StreamLoaderContext
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.to
import java.util.Objects

object Aff4ModelModule : KAbstractModule() {
  override fun configure() {
    bind(RealAff4Model.Loader::class.java)

    bind<Aff4Model.Loader>().to<RealAff4Model.Loader>()
    bind<Aff4StreamOpener>().to<RealAff4StreamOpener>()

    bindSet<Aff4StreamLoaderContext> { }
  }

  override fun equals(other: Any?): Boolean = javaClass == other?.javaClass

  override fun hashCode(): Int = Objects.hash(javaClass)
}
