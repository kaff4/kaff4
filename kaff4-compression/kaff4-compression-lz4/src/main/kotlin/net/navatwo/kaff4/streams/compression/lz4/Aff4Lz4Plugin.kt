package net.navatwo.kaff4.streams.compression.lz4

import net.navatwo.kaff4.plugins.KAff4Plugin

object Aff4Lz4Plugin : KAff4Plugin(pluginIdentifier = "kaff4:aff4-compression-lz4") {
  override fun configurePlugin() {
    bindCompressionMethods {
      to<Lz4Compression>()
    }
  }
}
