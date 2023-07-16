package net.navatwo.kaff4.model.rdf

import net.navatwo.kaff4.api.KAff4FeatureModule

object Aff4RdfModelFeatureModule : KAff4FeatureModule() {
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
