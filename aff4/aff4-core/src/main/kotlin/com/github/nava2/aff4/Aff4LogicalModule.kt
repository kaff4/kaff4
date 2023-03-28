package com.github.nava2.aff4

import com.github.nava2.aff4.streams.zip_segment.Aff4ZipSegmentModule
import com.github.nava2.guice.KAff4AbstractModule

object Aff4LogicalModule : KAff4AbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    install(Aff4ZipSegmentModule)
  }
}
