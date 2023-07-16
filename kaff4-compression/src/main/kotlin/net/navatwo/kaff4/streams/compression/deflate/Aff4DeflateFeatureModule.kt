package net.navatwo.kaff4.streams.compression.deflate

import net.navatwo.kaff4.api.KAff4FeatureModule

object Aff4DeflateFeatureModule : KAff4FeatureModule() {
  override fun configurePlugin() {
    bindCompressionMethods {
      to<DeflateCompression>()
    }
  }
}
