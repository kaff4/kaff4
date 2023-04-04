package net.navatwo.kaff4.streams.zip_segment

import com.google.inject.assistedinject.FactoryModuleBuilder
import net.navatwo.guice.build
import net.navatwo.guice.implement
import net.navatwo.guice.key
import net.navatwo.guice.typeLiteral
import net.navatwo.kaff4.model.rdf.ZipSegment
import net.navatwo.kaff4.streams.AbstractAff4StreamModule

internal object Aff4ZipSegmentModule : AbstractAff4StreamModule<ZipSegment, Aff4ZipSegmentSourceProvider>(
  configTypeLiteral = typeLiteral(),
  loaderKey = typeLiteral<Aff4ZipSegmentSourceProvider.Loader>().key,
) {
  override fun configureModule() {
    install(
      FactoryModuleBuilder()
        .implement<Aff4ZipSegmentSourceProvider, RealAff4ZipSegmentSourceProvider>()
        .build<Aff4ZipSegmentSourceProvider.Loader>()
    )
  }
}
