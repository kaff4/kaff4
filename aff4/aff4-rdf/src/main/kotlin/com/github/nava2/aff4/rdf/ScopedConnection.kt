package com.github.nava2.aff4.rdf

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Namespace
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.query.GraphQuery
import org.eclipse.rdf4j.query.Query
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.RepositoryResult
import org.eclipse.rdf4j.repository.util.RDFInserter
import org.eclipse.rdf4j.rio.ParserConfig
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings
import org.eclipse.rdf4j.rio.turtle.TurtleParser
import org.intellij.lang.annotations.Language
import java.io.InputStream

@RdfConnectionScoped
class ScopedConnection(
  private val connection: RepositoryConnection,
) {
  /** @see [RepositoryConnection.getValueFactory] */
  val valueFactory: ValueFactory by connection::valueFactory

  /** @see [RepositoryConnection.getParserConfig] */
  val parserConfig: ParserConfig by connection::parserConfig

  val namespaces: NamespacesProvider = NamespacesProvider(this, valueFactory)

  val mutable: Mutable = Mutable(connection)

  /** @see [RepositoryConnection.getNamespaces] */
  fun queryNamespaces(): RepositoryResult<Namespace> = connection.namespaces

  /** @see [RepositoryConnection.prepareQuery] */
  fun prepareQuery(@Language("SPARQL") query: String): Query = connection.prepareQuery(query)

  /** @see [RepositoryConnection.prepareQuery] */
  fun prepareQuery(ql: QueryLanguage, query: String): Query = connection.prepareQuery(ql, query)

  /** @see [RepositoryConnection.prepareTupleQuery] */
  fun prepareTupleQuery(@Language("SPARQL") query: String): TupleQuery = connection.prepareTupleQuery(query)

  /** @see [RepositoryConnection.prepareTupleQuery] */
  fun prepareTupleQuery(ql: QueryLanguage, query: String): TupleQuery = connection.prepareTupleQuery(ql, query)

  /** @see [RepositoryConnection.prepareGraphQuery] */
  fun prepareGraphQuery(@Language("SPARQL") query: String): GraphQuery = connection.prepareGraphQuery(query)

  /** @see [RepositoryConnection.prepareTupleQuery] */
  fun prepareGraphQuery(ql: QueryLanguage, query: String): GraphQuery = connection.prepareGraphQuery(ql, query)

  /** @see [RepositoryConnection.getStatements] */
  fun queryStatements(
    subj: Resource? = null,
    pred: IRI? = null,
    obj: Value? = null,
  ): RepositoryResult<Statement> = connection.getStatements(subj, pred, obj)

  internal fun close() = connection.close()

  class Mutable internal constructor(
    private val connection: RepositoryConnection,
  ) {
    fun addTurtle(input: InputStream) {
      val turtleParser = TurtleParser(connection.valueFactory)
      turtleParser.set(BasicParserSettings.VERIFY_URI_SYNTAX, false)

      turtleParser.rdfHandler = object : RDFInserter(connection) {
        override fun startRDF() {
          connection.begin()
          super.startRDF()
        }

        override fun endRDF() {
          super.endRDF()
          connection.commit()
        }
      }

      try {
        turtleParser.parse(input)
      } catch (@Suppress("TooGenericExceptionCaught") ex: Exception) {
        connection.rollback()
        throw ex
      }
    }

    /** @see [RepositoryConnection.add] */
    fun add(input: InputStream, dataFormat: RDFFormat, vararg contexts: Resource) {
      connection.add(input, dataFormat, *contexts)
    }

    /** @see [RepositoryConnection.add] */
    fun add(subject: Resource, vararg values: Pair<IRI, Value>) {
      val valueFactory = connection.valueFactory
      val statements = values.map { (p, o) -> valueFactory.createStatement(subject, p, o) }
      connection.add(statements)
    }

    /** @see [RepositoryConnection.setNamespace] */
    fun setNamespace(prefix: String, name: String) {
      connection.setNamespace(prefix, name)
    }
  }
}
