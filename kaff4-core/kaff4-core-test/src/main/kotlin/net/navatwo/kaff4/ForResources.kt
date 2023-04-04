package net.navatwo.kaff4

import javax.inject.Qualifier

@Target(
  AnnotationTarget.PROPERTY,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FIELD,
)
@Qualifier
annotation class ForResources
