package com.github.nava2.aff4.model.rdf

import com.github.nava2.aff4.plugins.KAff4Plugin

object Aff4RdfModelPlugin : KAff4Plugin(pluginIdentifier = "kaff4:aff4-rdf-models") {
  override fun configurePlugin() {
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
