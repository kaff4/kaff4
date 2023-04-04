package com.github.nava2.aff4

import com.github.nava2.aff4.model.dialect.Aff4LogicalStandardToolDialect
import com.github.nava2.aff4.model.dialect.DialectsModule
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.model.rdf.Aff4RdfModelPlugin
import com.github.nava2.guice.KAff4AbstractModule
import misk.scope.ActionScopedProvider
import misk.scope.ActionScopedProviderModule
import javax.inject.Inject
import javax.inject.Provider

object TestToolDialectModule : KAff4AbstractModule() {
  override fun configure() {
    install(DialectsModule)
    install(Aff4RdfModelPlugin)
    install(Aff4LogicalStandardToolDialect.Module)

    install(
      object : ActionScopedProviderModule() {
        override fun configureProviders() {
          bindProvider(ToolDialect::class, ToolDialectActionScopeProvider::class)
        }
      }
    )
  }

  private class ToolDialectActionScopeProvider @Inject constructor(
    private val provider: Provider<Aff4LogicalStandardToolDialect>,
  ) : ActionScopedProvider<ToolDialect> {
    override fun get(): Aff4LogicalStandardToolDialect = provider.get()
  }
}
