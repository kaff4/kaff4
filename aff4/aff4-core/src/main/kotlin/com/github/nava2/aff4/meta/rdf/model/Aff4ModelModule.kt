package com.github.nava2.aff4.meta.rdf.model

import com.github.nava2.aff4.meta.rdf.io.RdfValueConverter
import com.github.nava2.aff4.meta.rdf.parser.Aff4HashRdfValueConverter
import com.github.nava2.aff4.meta.rdf.parser.Aff4ImagePathRdfValueConverter
import com.github.nava2.guice.KAbstractModule
import kotlin.reflect.KClass

object Aff4ModelModule : KAbstractModule() {
  override fun configure() {
    bindSet<KClass<out Aff4RdfModel>> {
      for (subclass in Aff4RdfModel::class.sealedSubclasses) {
        toInstance(subclass)
      }
    }

    bindSet<RdfValueConverter<*>> {
      toInstance(Aff4ImagePathRdfValueConverter)
      toInstance(Aff4HashRdfValueConverter)
    }
  }
}
