package com.github.nava2.aff4.rdf.io

import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.rdf.io.literals.RdfLiteralConvertersModule
import com.github.nava2.guice.KAff4AbstractModule
import com.github.nava2.guice.assistedFactoryModule
import misk.scope.ActionScoped
import misk.scope.ActionScopedProvider
import misk.scope.ActionScopedProviderModule
import javax.inject.Inject

object RdfModelParserModule : KAff4AbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    install(RdfLiteralConvertersModule)
    install(assistedFactoryModule<RdfModelSerializer.Factory>())

    bind<RdfModelParser>().to<RealRdfModelParser>()
    install(object : ActionScopedProviderModule() {
      override fun configureProviders() {
        bindProvider(RdfAnnotationTypeInfo.Lookup::class, RdfAnnotationTypeInfoLookupProvider::class)
      }
    })
  }

  private class RdfAnnotationTypeInfoLookupProvider @Inject constructor(
    private val rdfAnnotationTypeInfo: RdfAnnotationTypeInfo.Lookup.Factory,
    private val toolDialectProvider: ActionScoped<ToolDialect>,
  ) : ActionScopedProvider<RdfAnnotationTypeInfo.Lookup> {
    override fun get(): RdfAnnotationTypeInfo.Lookup = rdfAnnotationTypeInfo.withDialect(toolDialectProvider.get())
  }
}
