package com.github.nava2.guice

import com.google.inject.AbstractModule
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.multibindings.MapBinder
import com.google.inject.multibindings.Multibinder
import kotlin.reflect.KClass

/**
 * Provides many Kotlin abstractions to ease binding content within an [AbstractModule].
 */
abstract class KAbstractModule protected constructor() : AbstractModule() {
  abstract override fun configure()

  protected inline fun <reified T> bind(): AnnotatedBindingBuilder<T> {
    return bind(T::class.java)
  }

  protected inline fun <reified T> requireBinding() {
    return requireBinding(T::class.java)
  }

  protected inline fun <reified T> requireBinding(annotatedWith: KClass<out Annotation>) {
    return requireBinding(key<T>(annotatedWith))
  }

  protected inline fun <reified T> bindSet(
    block: KSetMultibinderHelper<T>.() -> Unit
  ): Multibinder<T> {
    val multibinder = Multibinder.newSetBinder(binder(), typeLiteral<T>())
    KSetMultibinderHelper(multibinder).block()
    return multibinder
  }

  protected inline fun <reified T> bindSet(
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
