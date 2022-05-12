package com.github.nava2.aff4.meta.rdf.io

import com.github.nava2.guice.KAbstractModule
import com.google.inject.TypeLiteral
import com.google.inject.multibindings.MapBinder

internal object RdfModelParserModule : KAbstractModule() {
  override fun configure() {
    bindMap<TypeLiteral<*>, RdfValueConverter<*>> {
      bindConverter(StringRdfConverter)
      bindConverter(IntRdfConverter)
      bindConverter(LongRdfConverter)
      bindConverter(BigIntegerHandler)
      bindConverter(FloatRdfConverter)
      bindConverter(DoubleRdfConverter)
      bindConverter(ZonedDateTimeConverter)
      bindConverter(IriHandler)
      bindConverter(ResourceHandler)
    }
  }
}

fun MapBinder<in TypeLiteral<*>, in RdfValueConverter<*>>.bindConverter(converter: RdfValueConverter<*>) {
  for (type in converter.types) {
    addBinding(type).toInstance(converter)
  }
}
