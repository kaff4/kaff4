package net.navatwo.kaff4.model.rdf

/**
 * Defines a common interface for models that store binary data inside a container.
 */
interface StoredRdfModel : Aff4RdfModel {
  /**
   * Name of container this is stored in.
   */
  val stored: Aff4Arn
}
