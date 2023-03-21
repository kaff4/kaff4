package com.github.nava2.aff4.model

import com.github.nava2.aff4.model.rdf.Aff4Arn
import okio.FileSystem

data class Aff4Container(
  val containerArn: Aff4Arn,
  val dataFileSystem: FileSystem,
  val metadata: ToolMetadata,
) {
  data class ToolMetadata(
    val version: String,
    val tool: String,
  ) {
    companion object
  }
}
