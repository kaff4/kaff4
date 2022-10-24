package com.github.nava2.aff4

import com.github.nava2.guice.GuiceFactory
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage

internal object ChildInjectorGuiceFactory : GuiceFactory {
  override fun create(modules: Collection<Module>): Injector {
    return Guice.createInjector(Stage.DEVELOPMENT, modules)
  }
}
