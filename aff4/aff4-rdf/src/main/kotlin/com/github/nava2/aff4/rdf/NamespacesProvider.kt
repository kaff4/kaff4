package com.github.nava2.aff4.rdf

import com.github.nava2.aff4.model.rdf.TurtleIri
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory

class NamespacesProvider internal constructor(
  private val connection: RdfConnection,
  private val valueFactory: ValueFactory,
) {
  val defaultNamespace: String?
    get() = tryFindPrefix("")

  operator fun get(prefix: String): String? = tryFindPrefix(prefix)

  fun fromPrefix(prefix: String): String {
    return tryFindPrefix(prefix) ?: throw IllegalArgumentException("Prefix not found: $prefix")
  }

  fun iriFromTurtle(iri: TurtleIri): IRI {
    val (prefix, localName) = iri
    val namespace = fromPrefix(prefix)
    return valueFactory.createIRI(namespace, localName)
  }

  private fun tryFindPrefix(prefix: String): String? {
    return connection.queryNamespaces().use { iter ->
      iter.firstOrNull { it.prefix == prefix }?.name
    }
  }
}
