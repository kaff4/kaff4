package com.github.nava2.aff4.meta.rdf.io

@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RdfModel(val rdfType: String)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class RdfValue(val turtleRdfIri: String = USE_PROPERTY_NAME_MARKER) {
  companion object {
    const val USE_PROPERTY_NAME_MARKER = "||USE_PROPERTY_NAME||"
  }
}

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class RdfSubject
