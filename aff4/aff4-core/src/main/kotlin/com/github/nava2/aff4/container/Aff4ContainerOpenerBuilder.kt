package com.github.nava2.aff4.container

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.model.Aff4ContainerOpener
import com.github.nava2.guice.GuiceFactory
import com.google.inject.Module
import javax.inject.Inject

class Aff4ContainerOpenerBuilder @Inject internal constructor(
  private val guiceFactory: GuiceFactory,
) {
  private val featureModules = mutableSetOf<Module>(Aff4CoreModule)

  fun withFeatureModules(modules: Iterable<Module>): Aff4ContainerOpenerBuilder {
    featureModules += modules
    return this
  }

  fun withFeatureModules(vararg modules: Module): Aff4ContainerOpenerBuilder {
    featureModules += modules
    return this
  }

  fun build(): Aff4ContainerOpener {
    return RealAff4ContainerOpener(guiceFactory, featureModules)
  }
}
