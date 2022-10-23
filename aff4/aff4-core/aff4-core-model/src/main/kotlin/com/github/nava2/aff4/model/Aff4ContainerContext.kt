package com.github.nava2.aff4.model

import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.rdf.RdfExecutor
import okio.FileSystem

data class Aff4ContainerContext(
  val imageFileSystem: FileSystem,
  val containerArn: Aff4Arn,
  val metadata: Aff4Container.ToolMetadata,
  val rdfExecutor: RdfExecutor,
)
