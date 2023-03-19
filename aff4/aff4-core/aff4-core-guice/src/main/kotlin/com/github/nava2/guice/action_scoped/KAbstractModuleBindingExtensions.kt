package com.github.nava2.guice.action_scoped

import com.google.inject.binder.LinkedBindingBuilder
import kotlin.reflect.KClass

fun <T : Any> LinkedBindingBuilder<T>.inActionScope(klass: KClass<T>) {
  toProvider(ActionScope.seededKeyProvider(klass))
    .`in`(ActionScoped::class.java)
}

inline fun <reified T : Any> LinkedBindingBuilder<T>.inActionScope() = inActionScope(T::class)
