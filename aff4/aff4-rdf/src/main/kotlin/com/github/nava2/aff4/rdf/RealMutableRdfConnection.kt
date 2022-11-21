package com.github.nava2.aff4.rdf

import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings
import org.eclipse.rdf4j.rio.turtle.TurtleParser
import java.io.InputStream

internal class RealMutableRdfConnection @AssistedInject constructor(
  private val turtleReaderAndInserterFactory: TurtleReaderAndInserter.Factory,
  @Assisted private val connection: RepositoryConnection,
) : QueryableRdfConnection by RealRdfConnection(connection),
  MutableRdfConnection {

  override fun addTurtle(containerArn: Aff4Arn, input: InputStream) {
    val valueFactory = connection.valueFactory
    val turtleParser = TurtleParser(valueFactory)
    turtleParser.set(BasicParserSettings.VERIFY_URI_SYNTAX, false)

    turtleParser.rdfHandler = turtleReaderAndInserterFactory.create(containerArn, connection)

    try {
      turtleParser.parse(input)
    } catch (@Suppress("TooGenericExceptionCaught") ex: Exception) {
      connection.rollback()
      throw ex
    }
  }

  /** @see [RepositoryConnection.add] */
  override fun add(input: InputStream, dataFormat: RDFFormat, vararg contexts: Resource) {
    connection.add(input, dataFormat, *contexts)
  }

  /** @see [RepositoryConnection.add] */
  override fun add(subject: Resource, vararg values: Pair<IRI, Value>) {
    val valueFactory = connection.valueFactory
    val statements = values.map { (p, o) -> valueFactory.createStatement(subject, p, o) }
    connection.add(statements)
  }

  /** @see [RepositoryConnection.add] */
  override fun add(statements: Iterable<Statement>) {
    connection.add(statements)
  }

  /** @see [RepositoryConnection.add] */
  override fun add(statements: Sequence<Statement>) = add(statements.asIterable())

  /** @see [RepositoryConnection.setNamespace] */
  override fun setNamespace(prefix: String, name: String) {
    connection.setNamespace(prefix, name)
  }

  interface Factory {
    fun create(connection: RepositoryConnection): RealMutableRdfConnection
  }
}
