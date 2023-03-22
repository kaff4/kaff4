package com.github.nava2.aff4.dialects

import com.github.nava2.guice.KAbstractModule

object DialectsModule : KAbstractModule() {
  override fun configure() {
    install(Pyaff4Version11ToolDialect.Module)
  }
}
