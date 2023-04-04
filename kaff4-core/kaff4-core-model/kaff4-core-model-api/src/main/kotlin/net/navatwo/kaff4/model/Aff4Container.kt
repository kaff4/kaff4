package net.navatwo.kaff4.model

import net.navatwo.kaff4.model.rdf.Aff4Arn
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
