@file:Suppress("ForbiddenImport")

package com.github.nava2.guice.action_scoped

import com.github.nava2.guice.key
import com.google.common.annotations.VisibleForTesting
import com.google.inject.Key
import com.google.inject.OutOfScopeException
import com.google.inject.Scope
import com.google.inject.Scopes
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Provider
import kotlin.reflect.KClass
import com.google.inject.Provider as GuiceProvider

class ActionScope internal constructor() : Scope {
  override fun <T : Any> scope(key: Key<T>, unscoped: GuiceProvider<T>): GuiceProvider<T> {
    return GuiceProvider {
      val currentScopedMap = getCurrentSeedMap(key)

      var current = resolveScopedKeyOrNull(key)
      if (current == null && key !in currentScopedMap) {
        current = unscoped.get()

        // don't remember proxies; these exist only to serve circular dependencies
        if (Scopes.isCircularProxy(current)) {
          return@GuiceProvider current
        }

        currentScopedMap[key] = current
      }

      current
    }
  }

  fun start(seedMap: Map<Key<*>, Any>): Action {
    return Action(this, seedMap)
  }

  fun <R> runInNewScope(seedMap: Map<Key<*>, Any> = mapOf(), block: () -> R): R {
    return start(seedMap).use {
      block()
    }
  }

  @VisibleForTesting
  internal fun getSeedMap(actionKey: ActionKey, key: Key<*>? = null): MutableMap<Key<*>, Any> {
    return seedMaps[actionKey]
      ?: throw OutOfScopeException(
        buildString {
          append("Action $actionKey does not exist")
          if (key != null) {
            append(" while resolving key=$key")
          }
        }
      )
  }

  private fun getCurrentSeedMap(key: Key<*>? = null): MutableMap<Key<*>, Any> = getSeedMap(currentActionKey(key), key)

  private fun <T : Any> resolveScopedKeyOrNull(key: Key<T>): T? {
    val currentStack = scopeStacks.get()
    return currentStack.asReversed().firstNotNullOfOrNull { actionKey ->
      val seedMap = getSeedMap(actionKey, key = key)

      if (key in seedMap) {
        @Suppress("UNCHECKED_CAST")
        seedMap[key] as? T
      } else {
        null
      }
    }
  }

  @VisibleForTesting
  internal fun currentActionKey(key: Key<*>? = null): ActionKey {
    return scopeStacks.get().lastOrNull()
      ?: throw OutOfScopeException(
        buildString {
          append("Not in action scope, use [runInNewScope<>()]")
          if (key != null) {
            append(" failing to resolve $key")
          }
        }
      )
  }

  @VisibleForTesting
  internal fun computeFullCurrentSeedMap(): Map<Key<*>, Any> {
    val currentStack = scopeStacks.get() ?: throw OutOfScopeException("Not in action scope")
    return currentStack
      .fold(mutableMapOf()) { acc, actionKey ->
        acc += seedMaps.getValue(actionKey)
        acc
      }
  }

  internal data class ActionKey(val uuid: UUID) {
    companion object {
      fun generate(): ActionKey = ActionKey(UUID.randomUUID())
    }
  }

  private fun addDefaultSeeds(map: MutableMap<Key<*>, Any>, key: ActionKey) = map.apply {
    put(key<ActionKey>(), key)
    put(key<ActionScopedExecutors>(), ActionScopedExecutors(this@ActionScope))
  }

  class Action internal constructor(
    actionScope: ActionScope,
    seedMap: Map<Key<*>, Any>,
  ) : AutoCloseable {
    private val key: ActionKey = ActionKey.generate()
    private val currentStack = scopeStacks.get()

    init {
      seedMaps[key] = seedMap.toMutableMap().apply {
        actionScope.addDefaultSeeds(this, key)
      }
      currentStack.addLast(key)
    }

    override fun close() {
      val currentStack = scopeStacks.get()
      currentStack.remove(key)
      seedMaps.remove(key)
    }
  }

  internal data class Chain(
    val keys: List<ActionKey>,
  ) {
    fun <R> runInScope(actionScope: ActionScope, block: () -> R): R {
      scopeStacks.set(ArrayDeque(keys))

      try {
        return Action(actionScope, mapOf()).use {
          block()
        }
      } finally {
        scopeStacks.remove()
      }
    }
  }

  internal fun currentChain(): Chain = Chain(scopeStacks.get().toList())

  companion object {
    private val seedMaps = ConcurrentHashMap<ActionKey, MutableMap<Key<*>, Any>>()
    private val scopeStacks = ThreadLocal.withInitial<ArrayDeque<ActionKey>> { ArrayDeque() }

    /**
     * Returns a provider that always throws exception complaining that the object
     * in question must be seeded before it can be injected.
     *
     * @return typed provider
     */
    internal fun <T : Any> seededKeyProvider(klass: KClass<T>): Provider<T> {
      return Provider<T> {
        error(
          buildString {
            append("If you got here then it means that")
            append(" your code asked for scoped ${klass.qualifiedName} which should have been")
            append(" explicitly seeded in this scope by calling")
            append(" ${ActionScope::class.simpleName}.seed(), but was not.")
          }
        )
      }
    }
  }
}
