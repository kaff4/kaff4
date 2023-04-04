package com.github.nava2.aff4.model

import com.github.nava2.guice.KAff4AbstractModule
import com.github.nava2.guice.assistedFactoryModule
import com.github.nava2.guice.to
import java.util.Objects

object Aff4ModelModule : KAff4AbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    install(assistedFactoryModule<RealAff4Model.AssistedFactory>())
    bind<Aff4Model.Loader>().to<RealAff4Model.Loader>()
  }

  override fun equals(other: Any?): Boolean = javaClass == other?.javaClass

  override fun hashCode(): Int = Objects.hash(javaClass)
}
