package com.github.nava2.test

import com.github.nava2.aff4.io.Sha256FileSystemFactory
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.getInstance
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage
import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

open class GuiceTestRule(vararg val providedModules: Module) : MethodRule {

  private val baseModules = listOf<Module>(
    object : KAbstractModule() {
      override fun configure() {
        bind<Sha256FileSystemFactory>().toProvider(Provider { Sha256FileSystemFactory() })
      }
    }
  )

  private val modules = baseModules + providedModules

  override fun apply(base: Statement, method: FrameworkMethod, target: Any): Statement {
    val injector = Guice.createInjector(Stage.DEVELOPMENT, modules)
    injector.injectMembers(target)

    return object : Statement() {
      override fun evaluate() {
        injector.getInstance<CleanupActions>().use {
          evaluate(injector) {
            base.evaluate()
          }
        }
      }
    }
  }

  protected open fun evaluate(injector: Injector, block: () -> Unit) {
    block()
  }

  @Singleton
  protected class CleanupActions @Inject constructor() : AutoCloseable {
    private val cleanups = mutableListOf<() -> Unit>()

    fun register(action: () -> Unit) {
      cleanups += action
    }

    fun register(autoCloseable: AutoCloseable) {
      cleanups += autoCloseable::close
    }

    override fun close() {
      for (action in cleanups) {
        action()
      }
    }
  }
}
