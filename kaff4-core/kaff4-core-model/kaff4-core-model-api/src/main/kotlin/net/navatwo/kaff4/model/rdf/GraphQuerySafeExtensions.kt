package net.navatwo.kaff4.model.rdf

import net.navatwo.kaff4.api.InternalApi
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.query.GraphQuery

@InternalApi
fun GraphQuery.evaluateSequence(): Sequence<Statement> {
  return sequence {
    evaluate().use { yieldAll(it) }
  }
}
