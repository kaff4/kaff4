package com.github.nava2.aff4.meta

import com.github.nava2.aff4.meta.rdf.RdfConnectionScoped
import org.eclipse.rdf4j.model.Literal

data class BlockHashes(
  val iri: Iri,
  val hash: Hash,
) : Aff4Model {
  @RdfConnectionScoped
  class Parser: Aff4Model.Parser(
    types = listOf("aff4:BlockHashes"),
  ) {
    override fun protectedTryCreate(context: ModelRdfContext): BlockHashes {
      val hashValue = context.objectByIri<Literal>("aff4:hash")
      return BlockHashes(
        iri = context.iri,
        hash = Hash.fromLiteral(hashValue),
      )
    }
  }
}
