package com.github.nava2.aff4.rdf

import com.github.nava2.guice.KAbstractModule

object RdfConnectionScopeModule : KAbstractModule() {
  private val rdfConnectionScope = RdfConnectionScope()

  override fun configure() {
    bindScope(RdfConnectionScoped::class.java, rdfConnectionScope)
    bind<RdfConnectionScope>().toInstance(rdfConnectionScope)

    bind<ScopedConnection>()
      .toProvider(rdfConnectionScope.seededKeyProvider())
      .`in`(RdfConnectionScoped::class.java)

    bind<NamespacesProvider>()
      .toProvider(rdfConnectionScope.seededKeyProvider())
      .`in`(RdfConnectionScoped::class.java)
  }
}
