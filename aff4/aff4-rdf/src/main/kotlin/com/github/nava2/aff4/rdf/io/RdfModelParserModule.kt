package com.github.nava2.aff4.rdf.io

import com.github.nava2.aff4.rdf.io.literals.RdfLiteralConvertersModule
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.to

object RdfModelParserModule : KAbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    install(RdfLiteralConvertersModule)

    bind<RdfModelParser>().to<RealRdfModelParser>()
  }
}
