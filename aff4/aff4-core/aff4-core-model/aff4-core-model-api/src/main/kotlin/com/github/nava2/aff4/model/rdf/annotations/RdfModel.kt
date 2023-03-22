package com.github.nava2.aff4.model.rdf.annotations

import com.github.nava2.aff4.model.rdf.TurtleIri
import com.github.nava2.aff4.model.rdf.TurtleIri.Companion.toTurtleIri

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

val RdfModel.allRdfTypes: Set<TurtleIri>
  get() = if (synonyms.isEmpty()) {
    setOf(rdfType.toTurtleIri())
  } else {
    buildSet {
      add(rdfType.toTurtleIri())
      addAll(synonyms.asSequence().map { it.toTurtleIri() })
    }
  }
