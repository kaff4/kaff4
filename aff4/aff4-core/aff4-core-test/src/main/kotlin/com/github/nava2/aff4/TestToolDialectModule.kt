package com.github.nava2.aff4

import com.github.nava2.aff4.model.dialect.Aff4LogicalStandardToolDialect
import com.github.nava2.aff4.model.dialect.DefaultToolDialect
import com.github.nava2.aff4.model.dialect.DialectsModule
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.model.rdf.Aff4RdfModelPlugin
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.action_scoped.ActionScoped
import com.github.nava2.guice.key
import com.github.nava2.guice.to
import com.google.inject.Module
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.util.Modules

class TestToolDialectModule(
  customBindingProvider: LinkedBindingBuilder<ToolDialect>.() -> ScopedBindingBuilder = {
    to<Aff4LogicalStandardToolDialect>()
  },
) : Module by Modules.override(DialectsModule).with(
  object : KAbstractModule() {
    override fun configure() {
      install(Aff4RdfModelPlugin)
      install(Aff4LogicalStandardToolDialect.Module)

      bind(key<ToolDialect>(DefaultToolDialect::class))
        .customBindingProvider()

      bind<ToolDialect>()
        .customBindingProvider()
        .`in`(ActionScoped::class.java)
    }
  },
)