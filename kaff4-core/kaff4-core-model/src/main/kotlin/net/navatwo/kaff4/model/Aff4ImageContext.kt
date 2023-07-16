package net.navatwo.kaff4.model

import net.navatwo.kaff4.api.InternalApi
import net.navatwo.kaff4.rdf.RdfExecutor

@InternalApi
data class Aff4ImageContext(
  val imageName: String,
  val rdfExecutor: RdfExecutor,
  val containers: List<Aff4Container>,
)
