package net.navatwo.kaff4.rdf.io.literals

import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.kaff4.rdf.RdfValueConverter

internal object RdfLiteralConvertersModule : KAff4AbstractModule() {
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
