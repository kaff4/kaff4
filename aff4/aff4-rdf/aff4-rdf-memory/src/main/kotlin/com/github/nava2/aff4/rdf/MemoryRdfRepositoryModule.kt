package com.github.nava2.aff4.rdf

import com.github.nava2.guice.KAbstractModule

object MemoryRdfRepositoryModule : KAbstractModule() {
  override fun configure() {
    bind<RdfRepositoryConfiguration>().toInstance(MemoryRdfRepositoryConfiguration)
  }
}
