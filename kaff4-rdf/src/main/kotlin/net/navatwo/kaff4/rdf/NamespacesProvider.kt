package net.navatwo.kaff4.rdf

import net.navatwo.kaff4.model.rdf.TurtleIri
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
    return requireNotNull(tryFindPrefix(prefix)) { "Prefix not found: $prefix" }
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
