package com.github.nava2.aff4.container

import com.github.nava2.aff4.model.Aff4ContainerOpener
import com.google.inject.Injector
import com.google.inject.Module
import javax.inject.Inject

class Aff4ContainerOpenerBuilder @Inject internal constructor(
  private val parentInjector: Injector,
) {
  private val featureModules = mutableSetOf<Module>()

  fun withFeatureModules(modules: Iterable<Module>): Aff4ContainerOpenerBuilder {
    featureModules += modules
    return this
  }

  fun build(): Aff4ContainerOpener {
    return RealAff4ContainerOpener(parentInjector, featureModules)
  }
}
