package com.github.nava2.test

import com.github.nava2.configuration.TestConfigProviderModule
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.getInstance
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
    object : KAbstractModule() {
      override fun configure() {
        bindSet<AutoCloseable> { }
      }
    }
  )

  private val modules = baseModules + modules.toList()

  protected open fun setupInjector(
    base: Statement,
    method: FrameworkMethod,
    target: Any,
    injector: Injector,
  ): Injector = injector

  override fun apply(base: Statement, method: FrameworkMethod, target: Any): Statement {
    val injector = Guice.createInjector(Stage.DEVELOPMENT, modules)
    val testInjector = setupInjector(base, method, target, injector)
    testInjector.injectMembers(target)

    return object : Statement() {
      override fun evaluate() {
        try {
          base.evaluate()
        } finally {
          for (closable in testInjector.getInstance<Set<AutoCloseable>>()) {
            closable.close()
          }
        }
      }
    }
  }
}
