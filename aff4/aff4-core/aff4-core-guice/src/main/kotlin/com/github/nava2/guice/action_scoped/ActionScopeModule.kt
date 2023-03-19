package com.github.nava2.guice.action_scoped

import com.github.nava2.guice.KAbstractModule

object ActionScopeModule : KAbstractModule() {
  private val actionScope = ActionScope()

  override fun configure() {
    // tell Guice about the scope
    bindScope(ActionScoped::class.java, actionScope)

    bind<ActionScope>().toInstance(actionScope)

    bind(ActionScope.ActionKey.GUICE_ACTION_KEY).inActionScope()
  }
}
