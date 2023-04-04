package net.navatwo.guice

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.binder.ScopedBindingBuilder
import javax.inject.Provider
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified T> AnnotatedBindingBuilder<in T>.to(): ScopedBindingBuilder {
  return to(typeLiteral<T>())
}

inline fun <reified T> LinkedBindingBuilder<in T>.to(): ScopedBindingBuilder {
  return to(typeLiteral<T>())
}

operator fun <T> Provider<T>.getValue(instance: Any, property: KProperty<*>): T {
  return this.get()
}

inline fun <reified T> typeLiteral(): TypeLiteral<T> = object : TypeLiteral<T>() {}

inline fun <reified T> key(): Key<T> = Key.get(typeLiteral<T>())

inline fun <reified T> key(annotation: KClass<out Annotation>): Key<T> {
  return Key.get(typeLiteral<T>(), annotation.java)
}

val <T : Any> TypeLiteral<T>.key: Key<T>
  get() = Key.get(this)

inline fun <reified T : Any> Injector.getInstance(): T {
  return getInstance(key<T>())
}

inline fun <reified T : Any> Injector.getProvider(): Provider<T> {
  return getProvider(key<T>())
}
