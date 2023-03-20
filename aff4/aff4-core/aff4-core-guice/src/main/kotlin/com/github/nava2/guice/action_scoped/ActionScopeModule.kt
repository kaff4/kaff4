package com.github.nava2.guice.action_scoped

import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.key

object ActionScopeModule : KAbstractModule() {
  private val actionScope = ActionScope()

  override fun configure() {
    // tell Guice about the scope
    bindScope(ActionScoped::class.java, actionScope)

    bind<ActionScope>().toInstance(actionScope)

    bind(key<ActionScope.ActionKey>()).inActionScope()
    bind<ActionScopedExecutors>().inActionScope()
  }
}
