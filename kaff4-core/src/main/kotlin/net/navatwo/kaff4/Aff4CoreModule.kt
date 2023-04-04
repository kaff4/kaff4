package net.navatwo.kaff4

import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.kaff4.model.dialect.Aff4ToolDialectModule
import net.navatwo.kaff4.model.dialect.DialectsModule
import net.navatwo.kaff4.rdf.RdfRepositoryModule
import net.navatwo.kaff4.rdf.io.RdfModelParserModule

object Aff4CoreModule : KAff4AbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    install(RdfRepositoryModule)
    install(RdfModelParserModule)
    install(Aff4ToolDialectModule)
    install(DialectsModule)
  }
}
