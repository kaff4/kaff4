package com.github.nava2.test

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OffTestThreadExecutor @Inject internal constructor() : GuiceExtension.TestLifecycleAction {

  private lateinit var executorService: ExecutorService

  fun <T : Any?> synchronous(block: () -> T): T {
    return executorService.submit(block).get()
  }

  override fun beforeEach() {
    executorService = Executors.newSingleThreadExecutor { runnable ->
      Thread(runnable, OffTestThreadExecutor::class.simpleName!!)
    }
  }

  override fun afterEach() {
    executorService.shutdown()
  }
}
