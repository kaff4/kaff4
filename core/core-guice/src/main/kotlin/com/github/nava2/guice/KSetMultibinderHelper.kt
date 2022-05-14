package com.github.nava2.guice

import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.multibindings.Multibinder
import javax.inject.Provider

class KSetMultibinderHelper<T>(val multibinder: Multibinder<T>) {
  inline fun <reified U : T> to(): ScopedBindingBuilder = multibinder.addBinding().to(typeLiteral<U>())

  fun <U : T> toProvider(provider: Provider<U>) = multibinder.addBinding().toProvider(provider)

  fun <U : T> toInstance(instance: U) = multibinder.addBinding().toInstance(instance)
}
