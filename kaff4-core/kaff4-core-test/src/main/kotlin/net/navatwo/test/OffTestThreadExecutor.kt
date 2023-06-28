package net.navatwo.test

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Runs code in a separate thread. This is typically used to avoid collisions between action scopes in tests.
 */
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
