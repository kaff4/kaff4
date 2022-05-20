package com.github.nava2.aff4.model.rdf

import okio.Path
import okio.Path.Companion.toPath
import org.eclipse.rdf4j.model.IRI
import java.net.URLEncoder

fun IRI.toAff4Path(containerArn: IRI): Path {
  val cleanedPath = stringValue().substringAfter(containerArn.stringValue())
  val parts = cleanedPath.split('/')
  val encoded = parts.joinToString("/") {
    URLEncoder.encode(it, Charsets.UTF_8)
  }

  return encoded.toPath()
}
