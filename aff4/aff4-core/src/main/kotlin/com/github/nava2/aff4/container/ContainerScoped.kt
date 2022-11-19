package com.github.nava2.aff4.container

import com.google.inject.ScopeAnnotation
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Target(ANNOTATION_CLASS, CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@ScopeAnnotation
annotation class ContainerScoped