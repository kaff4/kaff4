package com.github.nava2.aff4.model

import com.github.nava2.guice.getInstance
import com.google.inject.Guice
import com.google.inject.Module
import com.google.inject.Stage

class Aff4ImageOpenerFactory(
  vararg val modules: Module,
) {
  fun create(): Aff4ImageOpener {
    val injector = Guice.createInjector(Stage.PRODUCTION, modules.toSet())
    return injector.getInstance()
  }
}
