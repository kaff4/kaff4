package com.github.nava2.aff4.meta.rdf

import com.github.nava2.guice.getInstance
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
  fun <T : Any, R> scoped(key: Key<T>, block: (instance: T) -> R): R {
    // TODO Pool connections
    return repository.connection.use { connection ->
      rdfConnectionScopeProvider.get().enterConnectionScope(connection).use {
        val instance = injector.getInstance(key)
        block(instance)
      }
    }
  }

  inline fun <reified T : Any, R> scoped(noinline block: (instance: T) -> R): R {
    return scoped(key(), block)
  }

  inline fun <reified T : Any, reified U : Any, R> scoped(noinline block: (T, U) -> R): R {
    return scoped { injector: Injector ->
      block(injector.getInstance(), injector.getInstance())
    }
  }
}
