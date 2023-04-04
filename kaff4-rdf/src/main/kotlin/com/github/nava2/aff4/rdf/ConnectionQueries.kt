package com.github.nava2.aff4.rdf

import com.github.nava2.aff4.model.rdf.TurtleIri
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource

fun RdfConnection.querySubjectsByType(type: IRI): List<Resource> {
  val query = queryStatements(pred = namespaces.iriFromTurtle(TurtleIri.RDF_TYPE), obj = type).apply {
    enableDuplicateFilter()
  }
  return query.use { result -> result.map { it.subject } }
}
