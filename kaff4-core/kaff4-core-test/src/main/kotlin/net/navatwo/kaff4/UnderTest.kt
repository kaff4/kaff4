package net.navatwo.kaff4

import jakarta.inject.Qualifier

/**
 * Defines a value that is provided from a test rule.
 */
@Target(
  AnnotationTarget.PROPERTY,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FIELD,
)
@Qualifier
annotation class UnderTest
