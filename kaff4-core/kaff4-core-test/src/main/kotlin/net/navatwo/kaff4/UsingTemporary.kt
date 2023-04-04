package net.navatwo.kaff4

@Target(
  AnnotationTarget.PROPERTY,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FIELD,
)
annotation class UsingTemporary(
  val useSha256: Boolean = false,
)
