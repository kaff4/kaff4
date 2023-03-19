package com.github.nava2.guice.action_scoped

import com.github.nava2.aff4.containsExactlyInAnyOrderEntriesOf
import com.github.nava2.test.GuiceModule
import com.google.inject.Key
import com.google.inject.OutOfScopeException
import com.google.inject.ProvisionException
import com.google.inject.name.Names
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import javax.inject.Inject
import javax.inject.Provider

class ActionScopedExecutorsTest {
  @GuiceModule
  val module = ActionScopeModule

  @Inject
  private lateinit var actionScope: ActionScope

  @Inject
  private lateinit var actionScopedExecutorsProvider: Provider<ActionScopedExecutors>

  @Test
  fun `trying to get executors out-of-scope throws OutOfScope exception`() {
    assertThatThrownBy {
      actionScopedExecutorsProvider.get()
    }.isInstanceOf(ProvisionException::class.java)
      .hasCauseInstanceOf(OutOfScopeException::class.java)
  }

  @Test
  fun `creating a task in an executor correctly copies the context chain and creates a new context per action`() {
    val parentKey = Key.get(Int::class.java, Names.named("parent"))
    actionScope.runInNewScope(mapOf(parentKey to 1)) {
      val parentActionKey = actionScope.currentActionKey()

      val childKey = Key.get(Int::class.java, Names.named("child"))
      actionScope.runInNewScope(mapOf(childKey to 2)) {
        val childActionKey = actionScope.currentActionKey()

        val executors = actionScopedExecutorsProvider.get()
        val executor = executors.newFixedThreadPool(nThreads = 1)
        try {
          executor.execute {
            assertThat(actionScope.currentActionKey())
              .isNotIn(parentActionKey, childActionKey)

            assertThat(actionScope.computeFullCurrentSeedMap())
              .containsExactlyInAnyOrderEntriesOf(
                parentKey to 1,
                childKey to 2,
              )

            val nestedKey = Key.get(Int::class.java, Names.named("nested"))
            actionScope.runInNewScope(mapOf(nestedKey to 3)) {
              assertThat(actionScope.computeFullCurrentSeedMap())
                .containsExactlyInAnyOrderEntriesOf(
                  parentKey to 1,
                  childKey to 2,
                  nestedKey to 3,
                )
            }

            assertThat(actionScope.computeFullCurrentSeedMap())
              .containsExactlyInAnyOrderEntriesOf(
                parentKey to 1,
                childKey to 2,
              )
          }
        } finally {
          executor.shutdown()
        }
      }
    }
  }
}
