package com.github.nava2.aff4.model.rdf.annotations

/**
 * Defines the [rdfType] of an [Aff4Model] type.
 */
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RdfModel(
  /**
   * Defines the expected RDF Type
   */
  val rdfType: String,
  /**
   * Defines synonyms for this type, this can be due to backwards compatibility.
   */
  vararg val synonyms: String,
)

val RdfModel.allRdfTypes: Set<String>
  get() = if (synonyms.isEmpty()) {
    setOf(rdfType)
  } else {
    buildSet {
      add(rdfType)
      addAll(synonyms)
    }
  }
