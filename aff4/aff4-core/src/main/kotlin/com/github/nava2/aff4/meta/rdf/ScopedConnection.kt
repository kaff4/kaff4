package com.github.nava2.aff4.meta.rdf

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
import org.eclipse.rdf4j.rio.ParserConfig
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.InputStream

@RdfConnectionScoped
data class ScopedConnection(
  private val connection: RepositoryConnection,
) {
  /** @see [RepositoryConnection.getValueFactory] */
  val valueFactory: ValueFactory by connection::valueFactory

  /** @see [RepositoryConnection.getParserConfig] */
  val parserConfig: ParserConfig by connection::parserConfig

  val namespaces: NamespacesProvider = NamespacesProvider(this, valueFactory)

  internal val mutable: Mutable = Mutable(connection)

  /** @see [RepositoryConnection.getNamespaces] */
  fun queryNamespaces(): RepositoryResult<Namespace> = connection.namespaces

  /** @see [RepositoryConnection.prepareQuery] */
  fun prepareQuery(query: String): Query = connection.prepareQuery(query)

  /** @see [RepositoryConnection.prepareQuery] */
  fun prepareQuery(ql: QueryLanguage, query: String): Query = connection.prepareQuery(ql, query)

  /** @see [RepositoryConnection.prepareTupleQuery] */
  fun prepareTupleQuery(query: String): TupleQuery = connection.prepareTupleQuery(query)

  /** @see [RepositoryConnection.prepareTupleQuery] */
  fun prepareTupleQuery(ql: QueryLanguage, query: String): TupleQuery = connection.prepareTupleQuery(ql, query)

  /** @see [RepositoryConnection.prepareGraphQuery] */
  fun prepareGraphQuery(query: String): GraphQuery = connection.prepareGraphQuery(query)

  /** @see [RepositoryConnection.prepareTupleQuery] */
  fun prepareGraphQuery(ql: QueryLanguage, query: String): GraphQuery = connection.prepareGraphQuery(ql, query)

  /** @see [RepositoryConnection.getStatements] */
  fun queryStatements(
    subj: Resource? = null,
    pred: IRI? = null,
    obj: Value? = null,
  ): RepositoryResult<Statement> = connection.getStatements(subj, pred, obj)

  internal fun close() = connection.close()

  data class Mutable internal constructor(
    private val connection: RepositoryConnection,
  ) {
    /** @see [RepositoryConnection.add] */
    fun add(input: InputStream, dataFormat: RDFFormat, vararg contexts: Resource) {
      connection.add(input, dataFormat, *contexts)
    }
  }
}
