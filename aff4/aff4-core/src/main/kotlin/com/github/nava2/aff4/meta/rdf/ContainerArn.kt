package com.github.nava2.aff4.meta.rdf

import com.google.inject.BindingAnnotation

/**
 * Defines the containers ARN.
 */
@Target(
  AnnotationTarget.PROPERTY,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FIELD,
)
@BindingAnnotation
annotation class ContainerArn
