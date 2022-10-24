package com.github.nava2.aff4.rdf.io

import com.github.nava2.aff4.rdf.io.literals.RdfLiteralConvertersModule
import com.github.nava2.guice.KAbstractModule

object RdfModelParserModule : KAbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    install(RdfLiteralConvertersModule)
  }
}
