package com.github.nava2.guice.action_scoped

import com.github.nava2.guice.action_scoped.assertj.containsAllEntriesOf
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
    val parentKey = Key.get(String::class.java, Names.named("parent"))
    val parentValue = "parent"
    actionScope.runInNewScope(mapOf(parentKey to parentValue)) {
      val parentActionKey = actionScope.currentActionKey()

      val childKey = Key.get(String::class.java, Names.named("child"))
      val childValue = "child"
      actionScope.runInNewScope(mapOf(childKey to childValue)) {
        val childActionKey = actionScope.currentActionKey()

        val executors = actionScopedExecutorsProvider.get()
        val executor = executors.newFixedThreadPool(nThreads = 1)
        var completedAction = false
        try {
          executor.submit {
            assertThat(actionScope.currentActionKey())
              .isNotIn(parentActionKey, childActionKey)

            assertThat(actionScope.computeFullCurrentSeedMap())
              .containsAllEntriesOf(
                parentKey to parentValue,
                childKey to childValue,
              )

            val nestedKey = Key.get(String::class.java, Names.named("nested"))
            val nestedValue = "nested"
            actionScope.runInNewScope(mapOf(nestedKey to nestedValue)) {
              assertThat(actionScope.computeFullCurrentSeedMap())
                .containsAllEntriesOf(
                  parentKey to parentValue,
                  childKey to childValue,
                  nestedKey to nestedValue,
                )
            }

            assertThat(actionScope.computeFullCurrentSeedMap())
              .containsAllEntriesOf(
                parentKey to parentValue,
                childKey to childValue,
              )

            completedAction = true
          }
            .get()
        } finally {
          executor.shutdown()
        }

        assertThat(completedAction)
          .`as` { "Action did not complete successfully" }
          .isTrue()
      }
    }
  }
}
