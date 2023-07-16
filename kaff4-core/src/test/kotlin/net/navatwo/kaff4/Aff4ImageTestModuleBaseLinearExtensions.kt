package net.navatwo.kaff4

import net.navatwo.kaff4.streams.compression.Aff4SnappyFeatureModule

val Aff4ImageTestModule.Companion.BaseLinear: Aff4ImageTestModule
  get() = object : Aff4ImageTestModule(imageName = "Base-Linear.aff4") {
    override fun configureOther() {
      install(Aff4SnappyFeatureModule)
    }
  }

val Aff4ImageTestModule.Companion.BaseLinearStriped: Aff4ImageTestModule
  get() = object : Aff4ImageTestModule(imageName = "base-linear_striped/Base-Linear_2.aff4") {
    override fun configureOther() {
      install(Aff4SnappyFeatureModule)
    }
  }
