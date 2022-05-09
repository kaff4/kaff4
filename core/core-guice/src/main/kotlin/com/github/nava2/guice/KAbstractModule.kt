package com.github.nava2.guice

import com.google.inject.AbstractModule
import com.google.inject.PrivateModule
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.binder.AnnotatedElementBuilder
import com.google.inject.multibindings.Multibinder

abstract class KAbstractModule protected constructor() : AbstractModule() {
  abstract override fun configure()

  protected inline fun <reified T> bind(): AnnotatedBindingBuilder<T> {
    return bind(T::class.java)
  }

  protected inline fun <reified T> bindSet(
    block: KSetMultibinderHelper<T>.() -> Unit
  ): Multibinder<T> {
    val multibinder = Multibinder.newSetBinder(binder(), T::class.java)
    KSetMultibinderHelper(multibinder).block()
    return multibinder
  }

  protected inline fun <reified T> bindSet(
    annotatedWith: Class<out Annotation>,
    block: KSetMultibinderHelper<T>.() -> Unit
  ): Multibinder<T> {
    val multibinder = Multibinder.newSetBinder(binder(), T::class.java, annotatedWith)
    KSetMultibinderHelper(multibinder).block()
    return multibinder
  }
}

abstract class KPrivateModule : PrivateModule() {
  abstract override fun configure()

  protected inline fun <reified T> bind(): AnnotatedBindingBuilder<T> {
    return bind(typeLiteral())
  }

  protected inline fun <reified T> expose(): AnnotatedElementBuilder {
    return expose(typeLiteral<T>())
  }

  protected inline fun <reified T> bindSet(
    block: KSetMultibinderHelper<T>.() -> Unit
  ): Multibinder<T> {
    val multibinder = Multibinder.newSetBinder(binder(), T::class.java)
    KSetMultibinderHelper(multibinder).block()
    return multibinder
  }

  protected inline fun <reified T> bindSet(
    annotatedWith: Class<out Annotation>,
    block: KSetMultibinderHelper<T>.() -> Unit
  ): Multibinder<T> {
    val multibinder = Multibinder.newSetBinder(binder(), T::class.java, annotatedWith)
    KSetMultibinderHelper(multibinder).block()
    return multibinder
  }
}
