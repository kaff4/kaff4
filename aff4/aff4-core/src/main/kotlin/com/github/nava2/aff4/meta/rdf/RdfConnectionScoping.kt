package com.github.nava2.aff4.meta.rdf

import com.github.nava2.guice.key
import com.google.inject.Injector
import com.google.inject.Key
import org.eclipse.rdf4j.repository.Repository
import javax.inject.Inject
import javax.inject.Provider

class RdfConnectionScoping @Inject constructor(
  private val rdfConnectionScopeProvider: Provider<RdfConnectionScope>,
  private val repository: Repository,
  private val injector: Injector,
) {
  fun <T : Any, R> scoped(key: Key<T>, block: (instance: T) -> R) {
    // TODO Pool connections
    repository.connection.use { connection ->
      rdfConnectionScopeProvider.get().enterConnectionScope(connection).use {
        val instance = injector.getInstance(key)
        block(instance)
      }
    }
  }

  inline fun <reified T : Any, R> scoped(noinline block: (instance: T) -> R) {
    return scoped(key(), block)
  }
}
