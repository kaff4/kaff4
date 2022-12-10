package com.github.nava2.aff4

import com.github.nava2.aff4.streams.compression.Aff4SnappyPlugin
import com.github.nava2.guice.KAbstractModule
import com.google.inject.Module

val Aff4ImageTestModule.Companion.BaseLinear: Module
  get() = object : KAbstractModule() {
    override fun configure() {
      install(Aff4ImageTestModule(imageName = "Base-Linear.aff4"))
      install(Aff4SnappyPlugin)
    }
  }

val Aff4ImageTestModule.Companion.BaseLinearStriped: Module
  get() = object : KAbstractModule() {
    override fun configure() {
      install(Aff4ImageTestModule(imageName = "base-linear_striped/Base-Linear_2.aff4"))
      install(Aff4SnappyPlugin)
    }
  }
