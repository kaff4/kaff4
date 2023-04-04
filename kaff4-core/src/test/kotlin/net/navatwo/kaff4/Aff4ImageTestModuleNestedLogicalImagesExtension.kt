package net.navatwo.kaff4

import net.navatwo.kaff4.model.dialect.Pyaff4Version11ToolDialect
import net.navatwo.kaff4.streams.compression.Aff4SnappyPlugin

val Aff4ImageTestModule.Companion.NestedLogicalImages: Aff4ImageTestModule
  get() = object : Aff4ImageTestModule(imageName = "nested_logical_images.aff4") {
    override fun configureOther() {
      install(Aff4SnappyPlugin)
      install(Aff4LogicalModule)
      install(Pyaff4Version11ToolDialect.Module)
    }
  }
