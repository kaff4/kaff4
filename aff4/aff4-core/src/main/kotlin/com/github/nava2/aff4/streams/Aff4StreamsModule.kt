package com.github.nava2.aff4.streams

import com.github.nava2.guice.KAbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder

internal object Aff4StreamsModule : KAbstractModule() {
  override fun configure() {
    install(FactoryModuleBuilder().build(BevyIndexReader.Factory::class.java))
    install(FactoryModuleBuilder().build(Aff4ImageBevies.Factory::class.java))
    install(FactoryModuleBuilder().build(Aff4ImageStream.Factory::class.java))
  }
}
