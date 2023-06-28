package net.navatwo.guice

import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.multibindings.Multibinder

class KSetMultibinderHelper<T>(val multibinder: Multibinder<T>) {
  inline fun <reified U : T> to(): ScopedBindingBuilder = multibinder.addBinding().to(typeLiteral<U>())

  fun <U : T> toInstance(instance: U) = multibinder.addBinding().toInstance(instance)
}
