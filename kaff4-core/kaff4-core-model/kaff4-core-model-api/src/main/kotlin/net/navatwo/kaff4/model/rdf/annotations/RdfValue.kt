package net.navatwo.kaff4.model.rdf.annotations

/**
 * Annotate properties to use a different turtle type ARN than the name of the property.
 *
 * For example:
 * ```
 * @RdfModel("aff4:ImageStream")
 * data class ImageStream(
 *   override val arn: Aff4Arn,
 *   @RdfValue("aff4:hash")
 *   val linearHashes: List<Hash> = listOf(),
 * )
 * ```
 *
 * When loading `linearHashes` from an RDF graph, it will be loaded from `aff4:hash` rather than
 * `aff4:linearHashes`.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class RdfValue(val turtleRdfIri: String)
