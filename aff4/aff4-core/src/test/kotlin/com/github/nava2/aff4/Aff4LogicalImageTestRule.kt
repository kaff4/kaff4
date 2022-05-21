package com.github.nava2.aff4

import com.google.inject.Module

class Aff4LogicalImageTestRule(vararg modules: Module, imageName: String = "dream.aff4") : Aff4ImageTestRule(
  modules = modules,
  imageName = imageName,
) {
  override val imageModules: List<Module> = listOf(
    Aff4LogicalModule,
  )
}
