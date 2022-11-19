package com.github.nava2.aff4.rdf.io.literals

import com.github.nava2.aff4.rdf.RdfValueConverter
import com.github.nava2.guice.KAbstractModule

internal object RdfLiteralConvertersModule : KAbstractModule() {
  override fun configure() {
    bindSet<RdfValueConverter<*>> {
      to<StringRdfConverter>()
      to<IntRdfConverter>()
      to<LongRdfConverter>()
      to<BigIntegerRdfConverter>()
      to<FloatRdfConverter>()
      to<DoubleRdfConverter>()
      to<ZonedDateTimeRdfConverter>()
      to<EnumRdfConverter>()
      to<IriRdfConverter>()
      to<ResourceRdfConverter>()
    }
  }
}
