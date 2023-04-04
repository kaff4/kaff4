package com.github.nava2.aff4.container

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.guice.KAff4AbstractModule
import com.github.nava2.guice.assistedFactoryModule
import com.github.nava2.guice.implement

internal object Aff4ContainerBuilderModule : KAff4AbstractModule() {
  override fun configure() {
    install(
      assistedFactoryModule<Aff4ContainerBuilder.Factory> {
        implement<Aff4ContainerBuilder, RealAff4ContainerBuilder>()
      }
    )

    install(assistedFactoryModule<Aff4StreamSinkFactory>())

    install(Aff4CoreModule)
  }
}
