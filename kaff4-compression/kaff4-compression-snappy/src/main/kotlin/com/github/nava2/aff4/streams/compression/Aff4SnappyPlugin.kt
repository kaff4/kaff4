package com.github.nava2.aff4.streams.compression

import com.github.nava2.aff4.plugins.KAff4Plugin

object Aff4SnappyPlugin : KAff4Plugin(pluginIdentifier = "kaff4:aff4-compression-snappy") {
  override fun configurePlugin() {
    bindCompressionMethods {
      to<SnappyCompression>()
    }
  }
}
