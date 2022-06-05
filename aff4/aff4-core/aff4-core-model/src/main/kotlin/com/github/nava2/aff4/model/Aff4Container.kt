package com.github.nava2.aff4.model

import java.io.Closeable

interface Aff4Container : Closeable {
  val aff4Model: Aff4Model
  val streamOpener: Aff4StreamOpener

  val metadata: ToolMetadata

  data class ToolMetadata(
    val version: String,
    val tool: String,
  ) {
    companion object
  }
}
