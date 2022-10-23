package com.github.nava2.guice

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.binder.ScopedBindingBuilder
import javax.inject.Provider
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

inline fun <reified T> key(): Key<T> = Key.get(typeLiteral())

val <T> TypeLiteral<T>.key: Key<T>
  get() = Key.get(this)

inline fun <reified T> Injector.getInstance(): T {
  return getInstance(typeLiteral<T>().key)
}

inline fun <reified T> Injector.getProvider(): Provider<T> {
  return getProvider(typeLiteral<T>().key)
}

inline fun <reified F> assistedFactoryModule(setupFn: FactoryModuleBuilder.() -> Unit = {}): Module {
  return FactoryModuleBuilder()
    .apply(setupFn)
    .build(F::class.java)
}

inline fun <reified T, reified U : T> FactoryModuleBuilder.implement(): FactoryModuleBuilder {
  return implement(T::class.java, U::class.java)
}
