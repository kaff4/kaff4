package com.github.nava2.guice

import com.google.inject.Injector
import com.google.inject.Module

/**
 * Provides a wrapper around [com.google.inject.Guice.createInjector]. This is helpful as we create injectors within
 * smaller contexts to avoid bleeding injection contexts.
 */
interface GuiceFactory {
  fun create(modules: Collection<Module>): Injector

  fun create(vararg modules: Module): Injector = create(modules.toSet())
}
