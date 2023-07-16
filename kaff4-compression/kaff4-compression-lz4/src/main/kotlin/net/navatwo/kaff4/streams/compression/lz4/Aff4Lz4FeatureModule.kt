package net.navatwo.kaff4.streams.compression.lz4

import net.navatwo.kaff4.api.KAff4FeatureModule

object Aff4Lz4FeatureModule : KAff4FeatureModule() {
  override fun configurePlugin() {
    bindCompressionMethods {
      to<Lz4Compression>()
    }
  }
}
