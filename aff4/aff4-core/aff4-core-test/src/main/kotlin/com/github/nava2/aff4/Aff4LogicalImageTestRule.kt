package com.github.nava2.aff4

import com.google.inject.Module

class Aff4LogicalImageTestRule(imageName: String, vararg modules: Module) : Aff4ImageTestRule(
  imageName = imageName,
  modules = (modules.toSet() + Aff4LogicalModule).toTypedArray(),
)
