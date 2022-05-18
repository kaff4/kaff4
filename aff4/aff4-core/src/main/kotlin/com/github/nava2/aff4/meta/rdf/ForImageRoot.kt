package com.github.nava2.aff4.meta.rdf

import com.google.inject.BindingAnnotation
import okio.FileSystem

/**
 * Defines a [FileSystem] that is rooted at the folder in which an image lives.
 */
@Target(
  AnnotationTarget.PROPERTY,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FIELD,
)
@BindingAnnotation
annotation class ForImageRoot
