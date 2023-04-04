package net.navatwo.kaff4.model.dialect

import net.navatwo.guice.KAff4AbstractModule

object Aff4ToolDialectModule : KAff4AbstractModule() {
  override fun configure() {
    requireBinding<ToolDialect>(DefaultToolDialect::class)

    bindSet<ToolDialect> { }
  }
}
