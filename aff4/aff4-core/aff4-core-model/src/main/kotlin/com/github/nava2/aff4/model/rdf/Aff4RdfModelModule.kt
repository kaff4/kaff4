package com.github.nava2.aff4.model.rdf

import com.github.nava2.aff4.rdf.io.RdfValueConverter
import com.github.nava2.guice.KAbstractModule
import kotlin.reflect.KClass

object Aff4RdfModelModule : KAbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    bindSet<KClass<out Aff4RdfModel>> {
      for (subclass in Aff4RdfModel::class.sealedSubclasses) {
        toInstance(subclass)
      }
    }

    bindSet<com.github.nava2.aff4.model.rdf.CompressionMethod> {}

    bindSet<RdfValueConverter<*>> {
      to<Aff4ImagePathRdfValueConverter>()
      to<Aff4HashRdfValueConverter>()
      to<Aff4CompressionMethodValueConverter>()
    }
  }
}
