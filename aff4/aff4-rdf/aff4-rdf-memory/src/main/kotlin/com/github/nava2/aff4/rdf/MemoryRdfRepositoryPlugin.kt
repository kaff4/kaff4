package com.github.nava2.aff4.rdf

import com.github.nava2.aff4.plugins.KAff4Plugin
import com.github.nava2.guice.to

object MemoryRdfRepositoryPlugin : KAff4Plugin(pluginIdentifier = "kaff4:aff4-rdf-memory") {
  override fun configurePlugin() {
    bindRdfRepositoryConfiguration().to<MemoryRdfRepositoryConfiguration>()
  }
}
