package com.github.nava2.test

import com.github.nava2.guice.GuiceFactory
import com.github.nava2.guice.KAbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage
import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement

open class GuiceTestRule(providedModules: Collection<Module>) : MethodRule {

  private val baseModules = listOf<Module>(
//    TestConfigProviderModule,
  )

  private val modules = baseModules + providedModules

  protected open fun setupInjector(
    injector: Injector,
    cleanupActions: CleanupActions,
  ): Injector = injector

  override fun apply(base: Statement, method: FrameworkMethod, target: Any): Statement {
    val cleanupActions = CleanupActions()
    val injector = Guice.createInjector(Stage.DEVELOPMENT, modules + TestModule)
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

  private object TestModule : KAbstractModule() {
    override fun configure() {
      bind<GuiceFactory>().toInstance(object : GuiceFactory {
        override fun create(modules: Collection<Module>): Injector {
          return Guice.createInjector(Stage.DEVELOPMENT, modules)
        }
      })
    }
  }

  constructor(vararg modules: Module) : this(modules.toList())
}
