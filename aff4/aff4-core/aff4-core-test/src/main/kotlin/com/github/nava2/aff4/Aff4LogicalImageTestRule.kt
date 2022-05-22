package com.github.nava2.aff4

import com.google.inject.Module

class Aff4LogicalImageTestRule(imageName: String = "dream.aff4", vararg modules: Module) : Aff4ImageTestRule(
  imageName = imageName,
  modules = modules,
) {
  override val imageModules: List<Module> = listOf(
    Aff4LogicalModule,
  )
}
