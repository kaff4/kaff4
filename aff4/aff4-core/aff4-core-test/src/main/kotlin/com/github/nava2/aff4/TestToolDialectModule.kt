package com.github.nava2.aff4

import com.github.nava2.aff4.model.dialect.Aff4LogicalStandardToolDialect
import com.github.nava2.aff4.model.dialect.DialectsModule
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.model.rdf.Aff4RdfModelPlugin
import com.github.nava2.guice.KAff4AbstractModule
import com.github.nava2.guice.action_scoped.ActionScoped
import javax.inject.Inject
import javax.inject.Provider

object TestToolDialectModule : KAff4AbstractModule() {
  override fun configure() {
    install(DialectsModule)
    install(Aff4RdfModelPlugin)
    install(Aff4LogicalStandardToolDialect.Module)

    bind<ToolDialect>()
      .toProvider(ToolDialectActionScopeProvider::class.java)
      .`in`(ActionScoped::class.java)
  }

  private class ToolDialectActionScopeProvider @Inject constructor(
    private val provider: Provider<Aff4LogicalStandardToolDialect>,
  ) : Provider<ToolDialect> {
    override fun get(): Aff4LogicalStandardToolDialect = provider.get()
  }
}
