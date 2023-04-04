package net.navatwo.kaff4.model

import com.google.inject.Guice
import com.google.inject.Module
import com.google.inject.Stage
import net.navatwo.guice.getInstance

class Aff4ImageOpenerFactory(
  vararg val modules: Module,
) {
  fun create(): Aff4ImageOpener {
    val injector = Guice.createInjector(Stage.PRODUCTION, modules.toSet())
    return injector.getInstance()
  }
}
