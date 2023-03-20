package com.github.nava2.guice.action_scoped

import com.github.nava2.guice.action_scoped.ActionScopingExecutorService.Companion.wrapExecutorInScope
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import javax.inject.Inject

@ActionScoped
class ActionScopedExecutors @Inject internal constructor(
  private val actionScope: ActionScope,
) {
  fun newCachedThreadPool(): ExecutorService {
    return actionScope.wrapExecutorInScope(Executors.newCachedThreadPool())
  }

  fun newCachedThreadPool(threadFactory: ThreadFactory): ExecutorService {
    return actionScope.wrapExecutorInScope(Executors.newCachedThreadPool(threadFactory))
  }

  fun newFixedThreadPool(nThreads: Int, threadFactory: ThreadFactory): ExecutorService {
    return actionScope.wrapExecutorInScope(Executors.newFixedThreadPool(nThreads, threadFactory))
  }

  fun newFixedThreadPool(nThreads: Int): ExecutorService {
    return actionScope.wrapExecutorInScope(Executors.newFixedThreadPool(nThreads))
  }
}
