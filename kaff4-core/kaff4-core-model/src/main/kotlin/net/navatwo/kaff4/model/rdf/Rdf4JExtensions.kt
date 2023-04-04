package net.navatwo.kaff4.model.rdf

import okio.Path
import okio.Path.Companion.toPath
import org.eclipse.rdf4j.model.IRI
import java.net.URLEncoder

/**
 * Converts an IRI into a storage path.
 *
 * https://github.com/aff4/Standard/blob/master/inprogress/AFF4StandardSpecification-v1.0a.md#5-storage-layer
 */
fun IRI.toAff4Path(storageArn: IRI): Path {
  val stringValue = stringValue()

  // 1. Strip the storage ARN if it exists
  val cleanedPath = stringValue.substringAfter(storageArn.stringValue())

  // 2. If the prefix exists, be sure to keep it in the first path, otherwise simple split
  val parts = if ("://" in cleanedPath.subSequence(0, "https://".length.coerceAtMost(cleanedPath.length))) {
    val (firstPartPrefix, rest) = cleanedPath.split("://")
    val restParts = rest.split('/')
    mutableListOf("$firstPartPrefix://${restParts.first()}") + restParts.drop(1)
  } else {
    cleanedPath.split('/')
  }

  val encoded = parts.joinToString("/") {
    URLEncoder.encode(it, Charsets.UTF_8)
  }

  return encoded.toPath()
}
