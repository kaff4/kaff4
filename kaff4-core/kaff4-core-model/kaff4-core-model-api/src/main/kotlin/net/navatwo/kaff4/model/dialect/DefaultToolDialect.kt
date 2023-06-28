package net.navatwo.kaff4.model.dialect

import jakarta.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * Used to define the default implementation for [ToolDialect]. This is used if no other specialty ones apply.
 */
@Target(VALUE_PARAMETER, PROPERTY_SETTER, FIELD)
@Retention(RUNTIME)
@Qualifier
annotation class DefaultToolDialect
