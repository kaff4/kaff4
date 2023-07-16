package net.navatwo.kaff4.rdf

import net.navatwo.kaff4.api.InternalApi
import net.navatwo.kaff4.model.rdf.Aff4Arn
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.InputStream

@InternalApi
interface MutableRdfConnection : QueryableRdfConnection {
  fun addTurtle(containerArn: Aff4Arn, input: InputStream)

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
