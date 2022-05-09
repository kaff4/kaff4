package com.github.nava2.aff4.meta

import com.github.nava2.aff4.meta.Iri.Companion.asIri
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.Value

data class ModelRdfContext(
//  val context: Context
  val namespaces: Namespaces,
  val iri: Iri,
  val statements: List<Statement>,
) {
  val types: Set<Iri>

  init {
    val rdfIri = Iri(namespaces.getValue("rdf"), "type")

    types = statements.asSequence()
      .filter { statement ->
        val predicate = statement.predicate
        predicate is IRI && predicate.asIri(namespaces) == rdfIri
      }
      .map { it.`object` }
      .filterIsInstance<IRI>()
      .map { it.asIri(namespaces) }
      .toSet()

    require(types.isNotEmpty()) {
      "No rdfTypes found: $statements"
    }
  }

  private val objectsByIri: Map<Iri, Value> = statements.associate { statement ->
    Iri(statement.predicate.namespace, statement.predicate.localName) to statement.`object`
  }

  fun <T : Value> objectByIri(prefixedName: String): T {
    @Suppress("UNCHECKED_CAST")
    return objectsByIri.getValue(Iri.parse(namespaces, prefixedName)) as T
  }
}