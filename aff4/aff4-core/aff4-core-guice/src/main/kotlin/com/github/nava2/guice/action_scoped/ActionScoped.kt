package com.github.nava2.guice.action_scoped

import com.google.inject.ScopeAnnotation
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Target(CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, VALUE_PARAMETER, FIELD)
@Retention(AnnotationRetention.RUNTIME)
@ScopeAnnotation
annotation class ActionScoped
