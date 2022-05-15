package com.github.nava2.aff4.meta.rdf.io

import com.github.nava2.guice.KAbstractModule

internal object RdfModelParserModule : KAbstractModule() {
  override fun configure() {
    bindSet<RdfValueConverter<*>> {
      toInstance(StringRdfConverter)
      toInstance(IntRdfConverter)
      toInstance(LongRdfConverter)
      toInstance(BigIntegerHandler)
      toInstance(FloatRdfConverter)
      toInstance(DoubleRdfConverter)
      toInstance(ZonedDateTimeConverter)
      toInstance(EnumConverter)
      toInstance(IriHandler)
      toInstance(ResourceHandler)
    }
  }
}
