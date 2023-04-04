package com.github.nava2.aff4.rdf.io

import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.rdf.io.literals.RdfLiteralConvertersModule
import com.github.nava2.guice.KAff4AbstractModule
import com.github.nava2.guice.action_scoped.ActionScoped
import com.github.nava2.guice.assistedFactoryModule
import javax.inject.Inject
import javax.inject.Provider

object RdfModelParserModule : KAff4AbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    install(RdfLiteralConvertersModule)
    install(assistedFactoryModule<RdfModelSerializer.Factory>())

    bind<RdfModelParser>().to<RealRdfModelParser>()

    bind<RdfAnnotationTypeInfo.Lookup>()
      .toProvider(RdfAnnotationTypeInfoLookupProvider::class.java)
      .`in`(ActionScoped::class.java)
  }

  private class RdfAnnotationTypeInfoLookupProvider @Inject constructor(
    private val rdfAnnotationTypeInfo: RdfAnnotationTypeInfo.Lookup.Factory,
    @ActionScoped private val toolDialectProvider: Provider<ToolDialect>,
  ) : Provider<RdfAnnotationTypeInfo.Lookup> {
    override fun get(): RdfAnnotationTypeInfo.Lookup = rdfAnnotationTypeInfo.withDialect(toolDialectProvider.get())
  }
}
