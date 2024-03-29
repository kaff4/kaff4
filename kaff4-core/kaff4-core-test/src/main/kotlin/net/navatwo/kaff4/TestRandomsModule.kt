package net.navatwo.kaff4

import com.google.inject.Module
import com.google.inject.util.Modules
import net.navatwo.guice.KAff4AbstractModule
import java.util.Random

object TestRandomsModule : Module by Modules.override(RandomsModule)
  .with(
    object : KAff4AbstractModule() {
      override fun configure() {
        bind<Random>().toProvider(com.google.inject.Provider { Random(0) })
      }
    }
  )
