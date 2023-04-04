package net.navatwo.kaff4.container

import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.guice.assistedFactoryModule
import net.navatwo.guice.implement
import net.navatwo.kaff4.Aff4CoreModule

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
