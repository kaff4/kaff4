package com.github.nava2.aff4.model.rdf.annotations

/**
 * Marks a property as the subject when loading an RDF graph.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class RdfSubject
