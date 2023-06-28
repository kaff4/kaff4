package net.navatwo.kaff4

import jakarta.inject.Qualifier

@Target(
  AnnotationTarget.PROPERTY,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FIELD,
)
@Qualifier
annotation class ForResources
