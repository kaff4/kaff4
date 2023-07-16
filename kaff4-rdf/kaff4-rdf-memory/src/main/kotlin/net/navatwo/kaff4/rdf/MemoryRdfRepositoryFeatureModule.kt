package net.navatwo.kaff4.rdf

import net.navatwo.kaff4.api.KAff4FeatureModule

object MemoryRdfRepositoryFeatureModule : KAff4FeatureModule() {
  override fun configurePlugin() {
    install(RdfRepositoryModule)

    bindRdfRepositoryConfiguration().to<MemoryRdfRepositoryConfiguration>()
  }
}
