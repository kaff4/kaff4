package net.navatwo.kaff4.rdf

import net.navatwo.guice.to
import net.navatwo.kaff4.plugins.KAff4Plugin

object MemoryRdfRepositoryPlugin : KAff4Plugin(pluginIdentifier = "kaff4:aff4-rdf-memory") {
  override fun configurePlugin() {
    install(RdfRepositoryModule)

    bindRdfRepositoryConfiguration().to<MemoryRdfRepositoryConfiguration>()
  }
}
