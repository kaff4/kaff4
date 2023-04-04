package net.navatwo.kaff4

import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.kaff4.streams.image_stream.Aff4ImageStreamModule
import net.navatwo.kaff4.streams.map_stream.Aff4MapStreamModule

object Aff4BaseStreamModule : KAff4AbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    install(Aff4ImageStreamModule)
    install(Aff4MapStreamModule)
  }
}
