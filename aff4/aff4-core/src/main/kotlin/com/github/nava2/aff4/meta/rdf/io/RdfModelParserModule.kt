package com.github.nava2.aff4.meta.rdf.io

import com.github.nava2.aff4.meta.rdf.io.RdfValueConverter.BigIntegerHandler
import com.github.nava2.aff4.meta.rdf.io.RdfValueConverter.DoubleHandler
import com.github.nava2.aff4.meta.rdf.io.RdfValueConverter.IntHandler
import com.github.nava2.aff4.meta.rdf.io.RdfValueConverter.IriHandler
import com.github.nava2.aff4.meta.rdf.io.RdfValueConverter.ResourceHandler
import com.github.nava2.aff4.meta.rdf.io.RdfValueConverter.StringHandler
import com.github.nava2.aff4.meta.rdf.io.RdfValueConverter.ZonedDateTimeConverter
import com.github.nava2.guice.KAbstractModule
import com.google.inject.TypeLiteral
import com.google.inject.multibindings.MapBinder

internal object RdfModelParserModule : KAbstractModule() {
  override fun configure() {
    bindMap<TypeLiteral<*>, RdfValueConverter<*>> {
      bindConverter(StringHandler)
      bindConverter(IntHandler)
      bindConverter(BigIntegerHandler)
      bindConverter(DoubleHandler)
      bindConverter(ZonedDateTimeConverter)
      bindConverter(IriHandler)
      bindConverter(ResourceHandler)
    }
  }
}

private fun MapBinder<in TypeLiteral<*>, in RdfValueConverter<*>>.bindConverter(converter: RdfValueConverter<*>) {
  addBinding(converter.type).toInstance(converter)
}
