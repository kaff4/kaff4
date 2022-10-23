package com.github.nava2.aff4.rdf

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.InputStream

interface MutableRdfConnection : QueryableRdfConnection {
  fun addTurtle(input: InputStream)

  /** @see [RepositoryConnection.add] */
  fun add(input: InputStream, dataFormat: RDFFormat, vararg contexts: Resource)

  /** @see [RepositoryConnection.add] */
  fun add(subject: Resource, vararg values: Pair<IRI, Value>)

  /** @see [RepositoryConnection.add] */
  fun add(statements: Iterable<Statement>)

  /** @see [RepositoryConnection.add] */
  fun add(statements: Sequence<Statement>) = add(statements.asIterable())

  /** @see [RepositoryConnection.setNamespace] */
  fun setNamespace(prefix: String, name: String)
}
