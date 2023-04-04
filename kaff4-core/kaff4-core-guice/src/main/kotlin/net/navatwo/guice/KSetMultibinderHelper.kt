package net.navatwo.guice

import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.multibindings.Multibinder
import javax.inject.Provider

class KSetMultibinderHelper<T>(val multibinder: Multibinder<T>) {
  inline fun <reified U : T> to(): ScopedBindingBuilder = multibinder.addBinding().to(typeLiteral<U>())

  fun <U : T> toProvider(provider: Provider<out U>) = multibinder.addBinding().toProvider(provider)

  fun <U : T> toProvider(provider: com.google.inject.Provider<out U>) = multibinder.addBinding().toProvider(provider)

  fun <U : T> toInstance(instance: U) = multibinder.addBinding().toInstance(instance)
}
