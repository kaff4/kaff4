package com.github.nava2.aff4.model.rdf

import com.github.nava2.aff4.model.Aff4LogicalStandardToolDialect
import com.github.nava2.aff4.model.Aff4LogicalStandardToolDialect.RdfStandardType
import com.github.nava2.aff4.model.dialect.Aff4ToolDialectModule
import com.github.nava2.aff4.model.dialect.DefaultToolDialect
import com.github.nava2.aff4.model.dialect.DialectTypeResolver
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.plugins.KAff4Plugin
import com.github.nava2.guice.key
import com.github.nava2.guice.to
import com.google.inject.Provides
import javax.inject.Singleton
import kotlin.reflect.KClass

object Aff4RdfModelPlugin : KAff4Plugin(pluginIdentifier = "kaff4:aff4-rdf-models") {
  override fun configurePlugin() {
    install(Aff4ToolDialectModule)

    bind(key<ToolDialect>(DefaultToolDialect::class))
      .to<Aff4LogicalStandardToolDialect>()

    bindAff4Models {
      for (subclass in Aff4RdfBaseModels::class.sealedSubclasses) {
        toInstance(subclass)
      }
    }

    bindRdfValueConverters {
      to<Aff4ImagePathRdfValueConverter>()
      to<Aff4HashRdfValueConverter>()
      to<Aff4CompressionMethodValueConverter>()
    }
  }

  @Singleton
  @Provides
  internal fun providesAff4LogicalStandardDialect(
    modelKlasses: Set<KClass<out Aff4RdfModel>>,
  ): Aff4LogicalStandardToolDialect {
    val typeResolver = DialectTypeResolver.Builder.forAnnotation(RdfStandardType::class) {
      rdfType to setOf(rdfType)
    }
      .apply {
        for (modelKlass in modelKlasses) {
          register(modelKlass)
        }
      }
      .build()

    return Aff4LogicalStandardToolDialect(typeResolver)
  }
}
