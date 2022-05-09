package com.github.nava2.aff4.meta

import org.eclipse.rdf4j.model.Literal
import javax.inject.Singleton

data class BlockHashes(
  val iri: Iri,
  val hash: Hash,
) : Aff4Model {
  @Singleton
  class Factory : Aff4Model.Factory(types = listOf("aff4:BlockHashes")) {
    override fun protectedTryCreate(context: ModelRdfContext): BlockHashes {
      val hashValue = context.objectByIri<Literal>("aff4:hash")
      return BlockHashes(
        iri = context.iri,
        hash = Hash.fromLiteral(hashValue),
      )
    }
  }
}
