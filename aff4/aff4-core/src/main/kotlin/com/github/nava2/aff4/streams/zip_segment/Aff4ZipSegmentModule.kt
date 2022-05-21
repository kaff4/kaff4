package com.github.nava2.aff4.streams.zip_segment

import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.aff4.streams.AbstractAff4StreamModule
import com.github.nava2.guice.key
import com.github.nava2.guice.typeLiteral
import com.google.inject.Key
import com.google.inject.assistedinject.FactoryModuleBuilder
import okio.FileSystem

internal object Aff4ZipSegmentModule : AbstractAff4StreamModule<ZipSegment, Aff4ZipSegment>(
  configTypeLiteral = typeLiteral(),
  loaderKey = typeLiteral<Aff4ZipSegment.Loader>().key,
) {
  override fun configureModule() {
    requireBinding(Key.get(FileSystem::class.java, ForImageRoot::class.java))

    install(FactoryModuleBuilder().build(Aff4ZipSegment.Loader::class.java))
  }
}
