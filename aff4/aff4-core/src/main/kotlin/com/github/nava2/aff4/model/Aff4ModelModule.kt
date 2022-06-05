package com.github.nava2.aff4.model

import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.to
import com.google.inject.assistedinject.FactoryModuleBuilder
import java.util.Objects

object Aff4ModelModule : KAbstractModule() {
  override fun configure() {
    install(FactoryModuleBuilder().build(RealAff4Model.AssistedFactory::class.java))
    bind<Aff4Model.Loader>().to<RealAff4Model.Loader>()
  }

  override fun equals(other: Any?): Boolean = javaClass == other?.javaClass

  override fun hashCode(): Int = Objects.hash(javaClass)
}
