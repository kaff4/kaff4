package com.github.nava2.aff4.meta

import com.github.nava2.aff4.meta.rdf.RdfConnectionScoped
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement

data class BlockHashes(
  val arn: Resource,
  val hash: Hash,
) : Aff4Model {
  @RdfConnectionScoped
  class Parser : Aff4Model.Parser<BlockHashes>(
    types = listOf("aff4:BlockHashes"),
  ) {
    private val hashIri by lazy { namespacesProvider.iriFromTurtle("aff4:hash") }

    override fun protectedTryCreate(subject: Resource, statements: List<Statement>): BlockHashes {
      val hashStatement = statements.singleOrNull { it.predicate == hashIri } ?: error("No $hashIri statement found")
      val hashValue = hashStatement.`object` as? Literal ?: error("Hash has invalid type: ${hashStatement.`object`}")

      return BlockHashes(
        arn = subject,
        hash = Hash.fromLiteral(hashValue),
      )
    }
  }
}
