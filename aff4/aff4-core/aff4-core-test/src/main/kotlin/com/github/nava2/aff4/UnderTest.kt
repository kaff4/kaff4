package com.github.nava2.aff4

import javax.inject.Qualifier

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
