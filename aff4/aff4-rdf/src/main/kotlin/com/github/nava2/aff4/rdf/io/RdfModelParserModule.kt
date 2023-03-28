package com.github.nava2.aff4.rdf.io

import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.rdf.io.literals.RdfLiteralConvertersModule
import com.github.nava2.guice.KAff4AbstractModule
import com.github.nava2.guice.action_scoped.ActionScoped
import com.github.nava2.guice.assistedFactoryModule
import com.github.nava2.guice.to
import com.google.inject.Provides

object RdfModelParserModule : KAff4AbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    install(RdfLiteralConvertersModule)
    install(assistedFactoryModule<RdfModelSerializer.Factory>())

    bind<RdfModelParser>().to<RealRdfModelParser>()
  }

  @Provides
  @ActionScoped
  internal fun providesRdfAnnotationTypeInfoLookup(
    rdfAnnotationTypeInfo: RdfAnnotationTypeInfo.Lookup.Factory,
    @ActionScoped toolDialect: ToolDialect,
  ): RdfAnnotationTypeInfo.Lookup = rdfAnnotationTypeInfo.withDialect(toolDialect)
}
