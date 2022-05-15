package com.github.nava2.aff4.meta.rdf

import com.google.inject.ScopeAnnotation
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@ScopeAnnotation
@Target(FUNCTION, PROPERTY, VALUE_PARAMETER, CLASS, FIELD, FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RdfConnectionScoped
