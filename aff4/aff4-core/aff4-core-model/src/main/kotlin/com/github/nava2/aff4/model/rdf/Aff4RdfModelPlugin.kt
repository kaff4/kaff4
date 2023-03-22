package com.github.nava2.aff4.model.rdf

import com.github.nava2.aff4.model.Aff4LogicalStandardToolDialect
import com.github.nava2.aff4.model.dialect.Aff4ToolDialectModule
import com.github.nava2.aff4.model.dialect.DefaultToolDialect
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.plugins.KAff4Plugin
import com.github.nava2.guice.key
import com.github.nava2.guice.to

object Aff4RdfModelPlugin : KAff4Plugin(pluginIdentifier = "kaff4:aff4-rdf-models") {
  override fun configurePlugin() {
    install(Aff4ToolDialectModule)
    install(Aff4LogicalStandardToolDialect.Module)

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
}
