package com.github.nava2.aff4.container

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.guice.KAbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder

internal object Aff4ContainerBuilderModule : KAbstractModule() {
  override fun configure() {
    install(
      FactoryModuleBuilder()
        .implement(Aff4ContainerBuilder::class.java, RealAff4ContainerBuilder::class.java)
        .build(Aff4ContainerBuilder.Factory::class.java)
    )

    install(
      FactoryModuleBuilder().build(Aff4StreamSinkFactory::class.java)
    )

    install(Aff4CoreModule)
  }
}
