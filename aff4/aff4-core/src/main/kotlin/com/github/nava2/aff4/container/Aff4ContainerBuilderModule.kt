package com.github.nava2.aff4.container

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.assistedFactoryModule
import com.github.nava2.guice.implement

internal object Aff4ContainerBuilderModule : KAbstractModule() {
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
