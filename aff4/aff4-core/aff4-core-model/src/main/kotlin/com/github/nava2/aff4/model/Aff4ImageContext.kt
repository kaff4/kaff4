package com.github.nava2.aff4.model

import com.github.nava2.aff4.rdf.RdfExecutor

data class Aff4ImageContext(
  val imageName: String,
  val rdfExecutor: RdfExecutor,
  val containers: List<Aff4Container>,
)
