package net.navatwo.kaff4.streams.compression.deflate

import net.navatwo.kaff4.plugins.KAff4Plugin

object Aff4DeflatePlugin : KAff4Plugin(pluginIdentifier = "kaff4:aff4-compression-deflate") {
  override fun configurePlugin() {
    bindCompressionMethods {
      to<DeflateCompression>()
    }
  }
}
