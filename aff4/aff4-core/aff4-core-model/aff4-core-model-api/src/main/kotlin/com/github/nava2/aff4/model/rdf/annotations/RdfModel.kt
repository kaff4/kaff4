package com.github.nava2.aff4.model.rdf.annotations

/**
 * Defines the [rdfType] of an [Aff4Model] type.
 */
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RdfModel(val rdfType: String)
