package net.navatwo.kaff4.rdf.io

import jakarta.inject.Inject
import misk.scope.ActionScoped
import misk.scope.ActionScopedProvider
import misk.scope.ActionScopedProviderModule
import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.guice.assistedFactoryModule
import net.navatwo.kaff4.model.dialect.ToolDialect
import net.navatwo.kaff4.rdf.io.literals.RdfLiteralConvertersModule

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
