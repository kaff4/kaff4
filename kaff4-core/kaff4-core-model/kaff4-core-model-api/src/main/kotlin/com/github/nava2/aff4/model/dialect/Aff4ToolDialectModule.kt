package com.github.nava2.aff4.model.dialect

import com.github.nava2.guice.KAff4AbstractModule

object Aff4ToolDialectModule : KAff4AbstractModule() {
  override fun configure() {
    requireBinding<ToolDialect>(DefaultToolDialect::class)

    bindSet<ToolDialect> { }
  }
}
