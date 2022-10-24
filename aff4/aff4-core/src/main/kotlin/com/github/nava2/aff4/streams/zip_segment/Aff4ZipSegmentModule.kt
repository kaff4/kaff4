package com.github.nava2.aff4.streams.zip_segment

import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.aff4.streams.AbstractAff4StreamModule
import com.github.nava2.guice.assistedFactoryModule
import com.github.nava2.guice.key
import com.github.nava2.guice.typeLiteral
import com.google.inject.Key
import okio.FileSystem

internal object Aff4ZipSegmentModule : AbstractAff4StreamModule<ZipSegment, Aff4ZipSegmentSourceProvider>(
  configTypeLiteral = typeLiteral(),
  loaderKey = typeLiteral<Aff4ZipSegmentSourceProvider.Loader>().key,
) {
  override fun configureModule() {
    requireBinding(Key.get(FileSystem::class.java, ForImageRoot::class.java))

    install(assistedFactoryModule<Aff4ZipSegmentSourceProvider.Loader>())
  }
}
