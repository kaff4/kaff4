package net.navatwo.kaff4

import net.navatwo.kaff4.model.dialect.Pyaff4Version11ToolDialect
import net.navatwo.kaff4.streams.compression.Aff4SnappyFeatureModule

val Aff4ImageTestModule.Companion.Dream: Aff4ImageTestModule
  get() = object : Aff4ImageTestModule(imageName = "dream.aff4") {
    override fun configureOther() {
      install(Aff4SnappyFeatureModule)
      install(Aff4LogicalModule)
      install(Pyaff4Version11ToolDialect.Module)
    }
  }
