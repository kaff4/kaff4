package com.github.nava2.aff4

import com.google.inject.BindingAnnotation

/**
 * Defines a value that is provided from a test rule.
 */
@Target(
  AnnotationTarget.PROPERTY,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FIELD,
)
@BindingAnnotation
annotation class UnderTest
