package net.navatwo.kaff4.rdf

import net.navatwo.kaff4.model.rdf.TurtleIri
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource

fun RdfConnection.querySubjectsByType(type: IRI): List<Resource> {
  val query = queryStatements(pred = namespaces.iriFromTurtle(TurtleIri.RDF_TYPE), obj = type).apply {
    enableDuplicateFilter()
  }
  return query.use { result -> result.map { it.subject } }
}
