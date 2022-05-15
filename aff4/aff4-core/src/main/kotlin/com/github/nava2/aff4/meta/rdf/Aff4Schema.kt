package com.github.nava2.aff4.meta.rdf

import okio.Path
import okio.Path.Companion.toPath
import org.eclipse.rdf4j.model.IRI
import java.net.URLEncoder

object Aff4Schema {
  const val SCHEMA = "http://aff4.org/Schema#"
}

fun IRI.toAff4Path(): Path {
  return URLEncoder.encode(stringValue(), Charsets.UTF_8).toPath()
}
