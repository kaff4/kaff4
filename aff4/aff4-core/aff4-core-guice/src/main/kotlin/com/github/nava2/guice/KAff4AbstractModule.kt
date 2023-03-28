package com.github.nava2.guice

import com.google.inject.AbstractModule
import com.google.inject.Binder
import com.google.inject.multibindings.MapBinder
import com.google.inject.multibindings.Multibinder
import misk.inject.KAbstractModule
import kotlin.reflect.KClass

/**
 * Provides many Kotlin abstractions to ease binding content within an [AbstractModule].
 */
abstract class KAff4AbstractModule protected constructor() : KAbstractModule() {
  abstract override fun configure()

  override fun binder(): Binder = super.binder().skipSources(KAff4AbstractModule::class.java)

  protected inline fun <reified T> requireBinding(annotatedWith: KClass<out Annotation>) {
    return requireBinding(key<T>(annotatedWith))
  }

  protected inline fun <reified T : Any> bindSet(
    block: KSetMultibinderHelper<T>.() -> Unit
  ): Multibinder<T> {
    val multibinder = Multibinder.newSetBinder(binder(), typeLiteral<T>())
    KSetMultibinderHelper(multibinder).block()
    return multibinder
  }

  protected inline fun <reified T : Any> bindSet(
    annotatedWith: KClass<out Annotation>,
    block: KSetMultibinderHelper<T>.() -> Unit
  ): Multibinder<T> {
    val multibinder = Multibinder.newSetBinder(binder(), typeLiteral<T>(), annotatedWith.java)
    KSetMultibinderHelper(multibinder).block()
    return multibinder
  }

  protected inline fun <reified T> bindSet(
    annotatedWith: Annotation,
    block: KSetMultibinderHelper<T>.() -> Unit
  ): Multibinder<T> {
    val multibinder = Multibinder.newSetBinder(binder(), typeLiteral<T>(), annotatedWith)
    KSetMultibinderHelper(multibinder).block()
    return multibinder
  }

  protected inline fun <reified K, reified V> bindMap(
    block: MapBinder<in K, in V>.() -> Unit
  ): MapBinder<K, V> {
    val mapBinder = MapBinder.newMapBinder(binder(), typeLiteral<K>(), typeLiteral<V>())
    block(mapBinder)
    return mapBinder
  }
}
