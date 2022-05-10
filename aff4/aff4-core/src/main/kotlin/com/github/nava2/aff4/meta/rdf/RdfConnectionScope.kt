package com.github.nava2.aff4.meta.rdf

import com.github.nava2.guice.key
import com.google.inject.Key
import com.google.inject.OutOfScopeException
import com.google.inject.Provider
import com.google.inject.Scope
import com.google.inject.Scopes
import org.eclipse.rdf4j.repository.RepositoryConnection

class RdfConnectionScope : Scope {

  private val currentContext = ThreadLocal<ConnectionScopeContext>()

  fun enterConnectionScope(connection: RepositoryConnection): AutoCloseable {
    check(currentContext.get() == null) {
      "Can not enter connections scope multiple times with the same connection: $connection"
    }

    val seededValues = mutableMapOf<Key<*>, Any>(
      key<RepositoryConnection>() to connection,
    )

    val closable = AutoCloseable {
      val context = currentContext.get()
      currentContext.remove()
      context?.seededValues?.clear()
    }

    currentContext.set(ConnectionScopeContext(connection, closable, seededValues))

    return closable
  }

  override fun <T : Any> scope(key: Key<T>, unscoped: Provider<T>): Provider<T> {
    return Provider<T> {
      val scopedObjects = getScopedObjectMap(key)

      @Suppress("UNCHECKED_CAST")
      val value = scopedObjects.getOrElse(key) { unscoped.get() } as T
      if (key in scopedObjects || Scopes.isCircularProxy(value)) {
        return@Provider value
      }

      scopedObjects[key] = value

      value
    }
  }

  /**
   * Returns a provider that always throws exception complaining that the object
   * in question must be seeded before it can be injected.
   *
   * @return typed provider
   */
  fun <T> seededKeyProvider(clazz: Class<T>): Provider<T> {
    return SeededKeyProvider(clazz)
  }

  /**
   * Returns a provider that always throws exception complaining that the object
   * in question must be seeded before it can be injected.
   *
   * @return typed provider
   */
  inline fun <reified T> seededKeyProvider(): Provider<T> {
    return seededKeyProvider(T::class.java)
  }

  private fun getScopedObjectMap(key: Key<*>): MutableMap<Key<*>, Any> {
    val currentContext = currentContext.get()
    val scopedObjectMap = currentContext?.seededValues
    return scopedObjectMap ?: throw OutOfScopeException("Cannot access $key outside of a connection scope")
  }

  private data class ConnectionScopeContext(
    val connection: RepositoryConnection,
    val closable: AutoCloseable,
    val seededValues: MutableMap<Key<*>, Any>,
  )

  private data class SeededKeyProvider<T>(private val clazz: Class<T>) : Provider<T> {
    override fun get(): T {
      error(
        "If you got here then it means that" +
          " your code asked for scoped object which should have been" +
          " explicitly seeded in this scope by calling" +
          " SimpleScope.seed(), but was not.",
      )
    }
  }
}