package com.github.nava2.aff4.model.dialect

import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.key
import com.github.nava2.guice.to

object DialectsModule : KAbstractModule() {
  override fun configure() {
    install(Aff4ToolDialectModule)

    install(Pyaff4Version11ToolDialect.Module)

    install(Aff4LogicalStandardToolDialect.Module)
    bind(key<ToolDialect>(DefaultToolDialect::class))
      .to<Aff4LogicalStandardToolDialect>()
  }
}
