package com.github.nava2.aff4

import com.github.nava2.aff4.streams.zip_segment.Aff4ZipSegmentModule
import com.github.nava2.guice.KAbstractModule

object Aff4LogicalModule : KAbstractModule() {
  override fun configure() {
    install(Aff4BaseStreamModule)

    install(Aff4ZipSegmentModule)
  }
}
