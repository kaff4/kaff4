package com.github.nava2.aff4.model.dialect

import com.google.inject.BindingAnnotation
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * Used to define the default implementation for [ToolDialect]. This is used if no other specialty ones apply.
 */
@Target(VALUE_PARAMETER, PROPERTY_SETTER, FIELD)
@Retention(RUNTIME)
@BindingAnnotation
annotation class DefaultToolDialect
