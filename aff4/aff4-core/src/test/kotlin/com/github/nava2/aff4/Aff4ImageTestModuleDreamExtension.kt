package com.github.nava2.aff4

import com.github.nava2.aff4.dialects.Pyaff4Version11ToolDialect
import com.github.nava2.aff4.streams.compression.Aff4SnappyPlugin
import com.google.inject.Module

val Aff4ImageTestModule.Companion.Dream: Aff4ImageTestModule
  get() = object : Aff4ImageTestModule(imageName = "dream.aff4") {
    override fun configureOther() {
      install(Aff4SnappyPlugin)
      install(Aff4LogicalModule)
      install(Pyaff4Version11ToolDialect.Module)
    }
  }
