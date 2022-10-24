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

interface QueryableRdfConnection {

  /** @see [RepositoryConnection.getValueFactory] */
  val valueFactory: ValueFactory

  /** @see [RepositoryConnection.getParserConfig] */
  val parserConfig: ParserConfig
  val namespaces: NamespacesProvider

  /** @see [RepositoryConnection.getNamespaces] */
  fun queryNamespaces(): RepositoryResult<Namespace>

  /** @see [RepositoryConnection.prepareQuery] */
  fun prepareQuery(@Language("SPARQL") query: String): Query

  /** @see [RepositoryConnection.prepareQuery] */
  fun prepareQuery(ql: QueryLanguage, query: String): Query

  /** @see [RepositoryConnection.prepareTupleQuery] */
  fun prepareTupleQuery(@Language("SPARQL") query: String): TupleQuery

  /** @see [RepositoryConnection.prepareTupleQuery] */
  fun prepareTupleQuery(ql: QueryLanguage, query: String): TupleQuery

  /** @see [RepositoryConnection.prepareGraphQuery] */
  fun prepareGraphQuery(@Language("SPARQL") query: String): GraphQuery

  /** @see [RepositoryConnection.prepareTupleQuery] */
  fun prepareGraphQuery(ql: QueryLanguage, query: String): GraphQuery

  /** @see [RepositoryConnection.getStatements] */
  fun queryStatements(
    subj: Resource? = null,
    pred: IRI? = null,
    obj: Value? = null,
  ): RepositoryResult<Statement>
}
