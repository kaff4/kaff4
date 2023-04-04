package net.navatwo.kaff4.model

import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.guice.assistedFactoryModule
import net.navatwo.guice.to
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
