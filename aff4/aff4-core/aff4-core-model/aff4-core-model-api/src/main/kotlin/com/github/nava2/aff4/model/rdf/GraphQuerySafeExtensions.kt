package com.github.nava2.aff4.model.rdf

import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.query.GraphQuery

fun GraphQuery.evaluateSequence(): Sequence<Statement> {
  return sequence {
    evaluate().use { yieldAll(it) }
  }
}