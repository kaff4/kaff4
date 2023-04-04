package net.navatwo.kaff4

import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.kaff4.streams.zip_segment.Aff4ZipSegmentModule

object Aff4LogicalModule : KAff4AbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    install(Aff4ZipSegmentModule)
  }
}
