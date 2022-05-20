package com.github.nava2.aff4.model.rdf

import okio.Path
import okio.Path.Companion.toPath
import org.eclipse.rdf4j.model.IRI
import java.net.URLEncoder

fun IRI.toAff4Path(): Path {
  return URLEncoder.encode(stringValue(), Charsets.UTF_8).toPath()
}
