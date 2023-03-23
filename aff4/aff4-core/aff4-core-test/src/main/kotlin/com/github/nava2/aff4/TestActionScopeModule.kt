package com.github.nava2.aff4

import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.action_scoped.ActionScope
import com.github.nava2.guice.action_scoped.ActionScopeModule
import com.github.nava2.test.GuiceExtension
import javax.inject.Inject

object TestActionScopeModule : KAbstractModule() {
  override fun configure() {
    install(ActionScopeModule)

    bindSet<GuiceExtension.TestLifecycleAction> {
      to<ActionScopeLifecycleAction>()
    }
  }

  private class ActionScopeLifecycleAction @Inject constructor(
    private val actionScope: ActionScope,
  ) : GuiceExtension.TestLifecycleAction {
    lateinit var action: ActionScope.Action

    override fun beforeEach() {
      if (::action.isInitialized) return
      action = actionScope.start(mapOf())
    }

    override fun afterEach() {
      if (!::action.isInitialized) return
      action.close()
    }
  }
}
