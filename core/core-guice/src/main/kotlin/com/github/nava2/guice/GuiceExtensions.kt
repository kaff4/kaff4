package com.github.nava2.guice

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.binder.ScopedBindingBuilder
import javax.inject.Provider
import kotlin.reflect.KProperty

inline fun <reified T> AnnotatedBindingBuilder<in T>.to(): ScopedBindingBuilder {
  return to(T::class.java)
}

operator fun <T> Provider<T>.getValue(instance: Any, property: KProperty<*>): T {
  return this.get()
}

inline fun <reified T> typeLiteral(): TypeLiteral<T> = object : TypeLiteral<T>() {}

inline fun <reified T> Injector.getInstance(): T {
  return getInstance(Key.get(typeLiteral()))
}
