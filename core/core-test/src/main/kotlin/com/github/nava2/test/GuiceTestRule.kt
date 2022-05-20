package com.github.nava2.test

import com.github.nava2.configuration.TestConfigProviderModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage
import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement

open class GuiceTestRule(vararg modules: Module) : MethodRule {
  private val baseModules = listOf(
    TestConfigProviderModule,
  )

  private val modules = baseModules + modules.toList()

  protected open fun setupInjector(
    injector: Injector,
    cleanupActions: CleanupActions,
  ): Injector = injector

  override fun apply(base: Statement, method: FrameworkMethod, target: Any): Statement {
    val cleanupActions = CleanupActions()
    val injector = Guice.createInjector(Stage.DEVELOPMENT, modules)
    val testInjector = setupInjector(injector, cleanupActions)
    testInjector.injectMembers(target)

    return object : Statement() {
      override fun evaluate() {
        cleanupActions.use {
          base.evaluate()
        }
      }
    }
  }

  protected class CleanupActions : AutoCloseable {
    private val cleanups = mutableListOf<() -> Unit>()

    fun register(action: () -> Unit) {
      cleanups += action
    }

    override fun close() {
      for (action in cleanups) {
        action()
      }
    }
  }
}
