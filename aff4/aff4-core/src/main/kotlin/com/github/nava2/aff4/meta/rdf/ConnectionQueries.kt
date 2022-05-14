package com.github.nava2.aff4.meta.rdf

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource

internal fun ScopedConnection.querySubjectsByType(type: IRI): List<Resource> {
  val query = queryStatements(pred = namespaces.iriFromTurtle("rdf:type"), obj = type).apply {
    enableDuplicateFilter()
  }
  return query.use { result -> result.map { it.subject } }
}