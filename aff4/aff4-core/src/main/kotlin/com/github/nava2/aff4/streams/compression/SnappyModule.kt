package com.github.nava2.aff4.streams.compression

import com.github.nava2.aff4.meta.rdf.model.CompressionMethod
import com.github.nava2.guice.KAbstractModule

object SnappyModule : KAbstractModule() {
  override fun configure() {
    bindSet<CompressionMethod> {
      to<SnappyCompression>()
    }
  }
}
