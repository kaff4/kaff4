package com.github.nava2.aff4.model.rdf

import com.github.nava2.aff4.rdf.io.RdfModel
import com.github.nava2.aff4.rdf.io.RdfValue
import okio.Path
import org.eclipse.rdf4j.model.IRI
import java.time.ZonedDateTime

@RdfModel("aff4:zip_segment")
data class ZipSegment(
  override val arn: IRI,
  val size: Long,
  @RdfValue("aff4:hash")
  val linearHashes: List<Hash>,
  val stored: IRI,
) : Aff4RdfModel {
  val segmentPath = arn.toAff4Path(stored)
}

@RdfModel("aff4:FileImage")
data class FileImage(
  override val arn: IRI,
  val originalFileName: Path,
  val size: Long,
  val birthTime: ZonedDateTime?,
  val lastAccessed: ZonedDateTime?,
  val lastWritten: ZonedDateTime?,
  val recordChanged: ZonedDateTime?,
) : Aff4RdfModel