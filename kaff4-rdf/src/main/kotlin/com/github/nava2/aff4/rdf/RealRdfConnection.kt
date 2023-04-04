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
import org.eclipse.rdf4j.rio.ParserConfig
import org.intellij.lang.annotations.Language

internal class RealRdfConnection(
  private val connection: RepositoryConnection,
) : RdfConnection {
  /** @see [RepositoryConnection.getValueFactory] */
  override val valueFactory: ValueFactory by connection::valueFactory

  /** @see [RepositoryConnection.getParserConfig] */
  override val parserConfig: ParserConfig by connection::parserConfig

  override val namespaces: NamespacesProvider = NamespacesProvider(this, valueFactory)

  /** @see [RepositoryConnection.getNamespaces] */
  override fun queryNamespaces(): RepositoryResult<Namespace> = connection.namespaces

  /** @see [RepositoryConnection.prepareQuery] */
  override fun prepareQuery(@Language("SPARQL") query: String): Query = connection.prepareQuery(query)

  /** @see [RepositoryConnection.prepareQuery] */
  override fun prepareQuery(ql: QueryLanguage, query: String): Query = connection.prepareQuery(ql, query)

  /** @see [RepositoryConnection.prepareTupleQuery] */
  override fun prepareTupleQuery(@Language("SPARQL") query: String): TupleQuery = connection.prepareTupleQuery(query)

  /** @see [RepositoryConnection.prepareTupleQuery] */
  override fun prepareTupleQuery(ql: QueryLanguage, query: String): TupleQuery = connection.prepareTupleQuery(ql, query)

  /** @see [RepositoryConnection.prepareGraphQuery] */
  override fun prepareGraphQuery(@Language("SPARQL") query: String): GraphQuery = connection.prepareGraphQuery(query)

  /** @see [RepositoryConnection.prepareTupleQuery] */
  override fun prepareGraphQuery(ql: QueryLanguage, query: String): GraphQuery = connection.prepareGraphQuery(ql, query)

  /** @see [RepositoryConnection.getStatements] */
  override fun queryStatements(
    subj: Resource?,
    pred: IRI?,
    obj: Value?,
  ): RepositoryResult<Statement> = connection.getStatements(subj, pred, obj)
}
