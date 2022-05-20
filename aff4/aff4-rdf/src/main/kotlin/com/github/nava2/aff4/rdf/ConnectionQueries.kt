package com.github.nava2.aff4.rdf

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource

fun ScopedConnection.querySubjectsByType(type: IRI): List<Resource> {
  val query = queryStatements(pred = namespaces.iriFromTurtle("rdf:type"), obj = type).apply {
    enableDuplicateFilter()
  }
  return query.use { result -> result.map { it.subject } }
}
