package com.github.nava2.aff4

import org.assertj.core.api.AbstractAssert
import java.util.function.Consumer

inline fun <SELF, ACTUAL> SELF.satisfies(crossinline consumer: (ACTUAL) -> Unit): SELF
  where SELF : AbstractAssert<out SELF, ACTUAL> = satisfies(Consumer { consumer(it) })

inline fun <reified T : Any> AbstractAssert<*, *>.isInstanceOf(): AbstractAssert<*, T> {
  @Suppress("UNCHECKED_CAST")
  return isInstanceOf(T::class.java) as AbstractAssert<*, T>
}
