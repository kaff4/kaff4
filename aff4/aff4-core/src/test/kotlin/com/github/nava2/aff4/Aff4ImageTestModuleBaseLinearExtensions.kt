package com.github.nava2.aff4

import com.github.nava2.aff4.streams.compression.Aff4SnappyPlugin
import com.google.inject.Module

val Aff4ImageTestModule.Companion.BaseLinear: Module
  get() = object : Aff4ImageTestModule(imageName = "Base-Linear.aff4") {
    override fun configureOther() {
      install(Aff4SnappyPlugin)
    }
  }

val Aff4ImageTestModule.Companion.BaseLinearStriped: Module
  get() = object : Aff4ImageTestModule(imageName = "base-linear_striped/Base-Linear_2.aff4") {
    override fun configureOther() {
      install(Aff4SnappyPlugin)
    }
  }
