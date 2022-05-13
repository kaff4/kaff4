package com.github.nava2.aff4.meta

import com.github.nava2.aff4.meta.parser.Aff4HashRdfValueConverter
import com.github.nava2.aff4.meta.parser.Aff4ImagePathRdfValueConverter
import com.github.nava2.aff4.meta.rdf.io.RdfValueConverter
import com.github.nava2.guice.KAbstractModule
import kotlin.reflect.KClass

object Aff4ModelModule : KAbstractModule() {
  override fun configure() {
    bindSet<KClass<out Aff4Model>> {
      for (subclass in Aff4Model::class.sealedSubclasses) {
        toInstance(subclass)
      }
    }

    bindSet<RdfValueConverter<*>> {
      toInstance(Aff4ImagePathRdfValueConverter)
      toInstance(Aff4HashRdfValueConverter)
    }
  }
}
