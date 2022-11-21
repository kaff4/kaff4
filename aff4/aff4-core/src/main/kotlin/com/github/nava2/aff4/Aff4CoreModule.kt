package com.github.nava2.aff4

import com.github.nava2.aff4.model.rdf.Aff4RdfModelPlugin
import com.github.nava2.aff4.rdf.RdfRepositoryModule
import com.github.nava2.aff4.rdf.io.RdfModelParserModule
import com.github.nava2.guice.KAbstractModule

object Aff4CoreModule : KAbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    install(RdfRepositoryModule)
    install(RdfModelParserModule)
    install(Aff4RdfModelPlugin)
  }

  override fun equals(other: Any?): Boolean = this === other

  override fun hashCode(): Int = javaClass.hashCode()
}
