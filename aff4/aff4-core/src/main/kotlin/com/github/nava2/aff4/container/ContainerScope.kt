@file:Suppress("ForbiddenImport")

package com.github.nava2.aff4.container

import com.google.inject.Key
import com.google.inject.OutOfScopeException
import com.google.inject.Scope
import com.google.inject.Scopes
import javax.inject.Provider
import com.google.inject.Provider as GuiceProvider

internal class ContainerScope : Scope {
  private val values = ThreadLocal<MutableMap<Key<*>, Any>>()

  fun enter() {
    check(values.get() == null) {
      "A scoping block is already in progress"
    }

    values.set(mutableMapOf())
  }

  fun exit() {
    checkNotNull(values.get()) {
      "No scoping block in progress"
    }

    values.remove()
  }

  fun <T : Any> seed(key: Key<T>, value: T) {
    val scopedObjects = getScopedObjectMap(key)

    check(key !in scopedObjects.keys) {
      "A value for the key $key was already seeded in this scope. Old: ${scopedObjects.getValue(key)}, New: $value"
    }

    scopedObjects[key] = value
  }

  override fun <T : Any> scope(key: Key<T>, unscoped: GuiceProvider<T>): GuiceProvider<T> {
    return GuiceProvider {
      val scopedObjects = getScopedObjectMap(key)

      @Suppress("UNCHECKED_CAST")
      var current = scopedObjects[key] as? T

      if (current == null && key !in scopedObjects) {
        current = unscoped.get()

        // don't remember proxies; these exist only to serve circular dependencies
        if (Scopes.isCircularProxy(current)) {
          return@GuiceProvider current
        }
        scopedObjects[key] = current
      }
      current
    }
  }

  private fun <T> getScopedObjectMap(key: Key<T>): MutableMap<Key<*>, Any> {
    return values.get()
      ?: throw OutOfScopeException(
        "Cannot access $key outside of a scoping block"
      )
  }

  companion object {
    /**
     * Returns a provider that always throws exception complaining that the object
     * in question must be seeded before it can be injected.
     *
     * @return typed provider
     */
    inline fun <reified T> seededKeyProvider(): Provider<T> {
      return Provider<T> {
        error(
          buildString {
            append("If you got here then it means that")
            append(" your code asked for scoped ${T::class.qualifiedName} which should have been")
            append(" explicitly seeded in this scope by calling")
            append(" ${ContainerScope::class.simpleName}.seed(), but was not.")
          }
        )
      }
    }
  }
}
