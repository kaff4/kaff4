package com.github.nava2.aff4.model.dialect

import com.github.nava2.guice.KAbstractModule

object Aff4ToolDialectModule : KAbstractModule() {
  override fun configure() {
    requireBinding<ToolDialect>(DefaultToolDialect::class)

    bindSet<ToolDialect> { }
  }
}
