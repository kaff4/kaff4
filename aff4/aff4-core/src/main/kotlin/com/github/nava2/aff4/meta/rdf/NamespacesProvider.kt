package com.github.nava2.aff4.meta.rdf

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory

class NamespacesProvider internal constructor(
  private val connection: ScopedConnection,
  private val valueFactory: ValueFactory,
) {
  val defaultNamespace: String?
    get() = tryFindPrefix("")

  operator fun get(prefix: String): String? = tryFindPrefix(prefix)

  fun fromPrefix(prefix: String): String {
    return tryFindPrefix(prefix) ?: throw IllegalArgumentException("Prefix not found: $prefix")
  }

  fun iriFromTurtle(turtleIri: String): IRI {
    val (prefix, localName) = turtleIri.split(':')
    val namespace = fromPrefix(prefix)
    return valueFactory.createIRI(namespace, localName)
  }

  private fun tryFindPrefix(prefix: String): String? {
    return connection.queryNamespaces().use { iter ->
      iter.firstOrNull { it.prefix == prefix }?.name
    }
  }
}
