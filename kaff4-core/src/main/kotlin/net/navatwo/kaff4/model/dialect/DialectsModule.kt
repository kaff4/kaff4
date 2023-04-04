package net.navatwo.kaff4.model.dialect

import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.guice.key
import net.navatwo.guice.to

object DialectsModule : KAff4AbstractModule() {
  override fun configure() {
    install(Aff4ToolDialectModule)

    install(Pyaff4Version11ToolDialect.Module)

    install(Aff4LogicalStandardToolDialect.Module)
    bind(key<ToolDialect>(DefaultToolDialect::class))
      .to<Aff4LogicalStandardToolDialect>()
  }
}
