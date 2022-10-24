package com.github.nava2.aff4.rdf

import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.to

object MemoryRdfRepositoryModule : KAbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()

    bind<RdfRepositoryConfiguration>().to<MemoryRdfRepositoryConfiguration>()
  }
}
