package com.github.nava2.aff4

import com.github.nava2.aff4.model.dialect.Aff4ToolDialectModule
import com.github.nava2.aff4.model.dialect.DialectsModule
import com.github.nava2.aff4.rdf.RdfRepositoryModule
import com.github.nava2.aff4.rdf.io.RdfModelParserModule
import com.github.nava2.guice.KAff4AbstractModule
import com.github.nava2.guice.action_scoped.ActionScopeModule

object Aff4CoreModule : KAff4AbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    install(RdfRepositoryModule)
    install(RdfModelParserModule)
    install(Aff4ToolDialectModule)
    install(ActionScopeModule)
    install(DialectsModule)
  }
}
