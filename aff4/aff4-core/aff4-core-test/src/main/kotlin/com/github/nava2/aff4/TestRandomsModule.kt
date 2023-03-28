package com.github.nava2.aff4

import com.github.nava2.guice.KAff4AbstractModule
import com.google.inject.Module
import com.google.inject.util.Modules
import java.util.Random
import javax.inject.Provider

object TestRandomsModule : Module by Modules.override(RandomsModule)
  .with(
    object : KAff4AbstractModule() {
      override fun configure() {
        bind<Random>().toProvider(Provider { Random(0) })
      }
    }
  )
