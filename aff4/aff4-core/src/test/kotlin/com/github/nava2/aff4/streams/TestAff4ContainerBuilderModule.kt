package com.github.nava2.aff4.streams

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.TestRandomsModule
import com.github.nava2.aff4.container.Aff4ContainerBuilderModule
import com.github.nava2.guice.KAbstractModule

object TestAff4ContainerBuilderModule : KAbstractModule() {
  override fun configure() {
    install(Aff4ContainerBuilderModule)
    install(TestRandomsModule)
    install(Aff4CoreModule)
  }
}
