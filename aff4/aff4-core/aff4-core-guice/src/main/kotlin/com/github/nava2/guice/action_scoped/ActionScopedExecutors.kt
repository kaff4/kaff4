package com.github.nava2.guice.action_scoped

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

object ActionScopedExecutors {
  fun newCachedThreadPool(): ExecutorService {
    return ActionScope.wrapExecutorInScope(Executors.newCachedThreadPool())
  }

  fun newCachedThreadPool(threadFactory: ThreadFactory): ExecutorService {
    return ActionScope.wrapExecutorInScope(Executors.newCachedThreadPool(threadFactory))
  }
}
