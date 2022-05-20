package com.github.nava2.aff4

suspend fun <T> SequenceScope<T>.yieldNotNull(nullableValue: T?) {
  if (nullableValue != null) {
    yield(nullableValue)
  }
}
