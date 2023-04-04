package com.github.nava2.aff4

import com.github.nava2.guice.KAff4AbstractModule
import com.github.nava2.test.GuiceExtension
import misk.scope.ActionScope
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

object TestActionScopeModule : KAff4AbstractModule() {
  override fun configure() {
    bindSet<GuiceExtension.TestLifecycleAction> {
      to<ActionScopeLifecycleAction>()
    }
  }

  @Singleton
  private class ActionScopeLifecycleAction @Inject constructor(
    private val actionScopeProvider: Provider<ActionScope>,
  ) : GuiceExtension.TestLifecycleAction {
    private lateinit var action: AutoCloseable

    override fun beforeEach() {
      check(!::action.isInitialized) { "Action is already setup" }
      action = actionScopeProvider.get().enter(mapOf())
    }

    override fun afterEach() {
      action.close()
    }
  }
}
