package com.github.nava2.guice.action_scoped

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.RunnableFuture
import java.util.concurrent.TimeUnit

internal class ActionScopingExecutorService constructor(
  private val actionScope: ActionScope,
  private val actionChain: ActionScope.Chain,
  private val delegateExecutorService: ExecutorService,
) : AbstractExecutorService() {
  override fun execute(command: Runnable) {
    return delegateExecutorService.execute(command)
  }

  override fun <T : Any?> newTaskFor(callable: Callable<T>): RunnableFuture<T> {
    return super.newTaskFor {
      actionChain.runInScope(actionScope) {
        callable.call()
      }
    }
  }

  override fun <T : Any?> newTaskFor(runnable: Runnable, value: T): RunnableFuture<T> {
    val wrappedRunnable = Runnable {
      actionChain.runInScope(actionScope) {
        runnable.run()
      }
    }
    return super.newTaskFor(wrappedRunnable, value)
  }

  override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
    return delegateExecutorService.awaitTermination(timeout, unit)
  }

  override fun isShutdown(): Boolean = delegateExecutorService.isShutdown

  override fun shutdown() {
    delegateExecutorService.shutdown()
  }

  override fun shutdownNow(): List<Runnable> = delegateExecutorService.shutdownNow()

  override fun isTerminated(): Boolean = delegateExecutorService.isTerminated

  companion object {
    fun ActionScope.wrapExecutorInScope(executorService: ExecutorService): ExecutorService {
      return ActionScopingExecutorService(this, currentChain(), executorService)
    }
  }
}
