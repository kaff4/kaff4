package com.github.nava2.aff4.streams.compression

import com.github.nava2.aff4.model.rdf.CompressionMethod
import com.github.nava2.guice.KAbstractModule

object Aff4SnappyModule : KAbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    bindSet<CompressionMethod> {
      to<SnappyCompression>()
    }
  }
}
