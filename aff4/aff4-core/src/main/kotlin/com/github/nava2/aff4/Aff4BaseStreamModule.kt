package com.github.nava2.aff4

import com.github.nava2.aff4.streams.image_stream.Aff4ImageStreamModule
import com.github.nava2.aff4.streams.map_stream.Aff4MapStreamModule
import com.github.nava2.guice.KAbstractModule

object Aff4BaseStreamModule : KAbstractModule() {
  override fun configure() {
    install(Aff4ImageStreamModule)
    install(Aff4MapStreamModule)
  }
}
