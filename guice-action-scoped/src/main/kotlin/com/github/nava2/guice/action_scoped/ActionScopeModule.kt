package com.github.nava2.guice.action_scoped

import com.google.inject.AbstractModule
import com.google.inject.Key
import com.google.inject.binder.AnnotatedBindingBuilder

object ActionScopeModule : AbstractModule() {
  private val actionScope = ActionScope()

  override fun configure() {
    bindScope(ActionScoped::class.java, actionScope)

    bind<ActionScope>().toInstance(actionScope)

    bind(Key.get(ActionScope.ActionKey::class.java)).inActionScope()
    bind<ActionScopedExecutors>().inActionScope()
  }

  private inline fun <reified T> bind(): AnnotatedBindingBuilder<T> {
    return bind(T::class.java)
  }
}
