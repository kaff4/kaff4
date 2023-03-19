package com.github.nava2.aff4

import org.assertj.core.api.AbstractAssert
import java.util.function.Consumer

inline fun <SELF, ACTUAL> AbstractAssert<SELF, ACTUAL>.satisfies(crossinline consumer: (ACTUAL) -> Unit): SELF
  where SELF : AbstractAssert<SELF, ACTUAL> = satisfies(Consumer { consumer(it) })
