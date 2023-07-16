package net.navatwo.kaff4.streams.compression

import net.navatwo.kaff4.api.KAff4FeatureModule

object Aff4SnappyFeatureModule : KAff4FeatureModule() {
  override fun configurePlugin() {
    bindCompressionMethods {
      to<SnappyCompression>()
    }
  }
}
