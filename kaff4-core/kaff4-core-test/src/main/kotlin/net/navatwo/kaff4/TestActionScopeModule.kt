package net.navatwo.kaff4

import misk.scope.ActionScope
import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.test.GuiceExtension
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
