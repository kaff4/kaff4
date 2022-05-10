package com.github.nava2.aff4.meta.rdf

import com.github.nava2.guice.KAbstractModule
import org.eclipse.rdf4j.repository.RepositoryConnection

object RdfConnectionScopeModule : KAbstractModule() {
  private val rdfConnectionScope = RdfConnectionScope()

  override fun configure() {
    bindScope(RdfConnectionScoped::class.java, rdfConnectionScope)
    bind<RdfConnectionScope>().toInstance(rdfConnectionScope)

    bind<RepositoryConnection>()
      .toProvider(rdfConnectionScope.seededKeyProvider())
      .`in`(RdfConnectionScoped::class.java)
  }
}