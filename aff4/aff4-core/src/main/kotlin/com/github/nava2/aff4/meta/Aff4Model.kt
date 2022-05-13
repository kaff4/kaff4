package com.github.nava2.aff4.meta

import com.github.nava2.aff4.meta.rdf.io.RdfModel
import com.github.nava2.aff4.meta.rdf.io.RdfSubject
import com.github.nava2.aff4.meta.rdf.io.RdfValue
import okio.Path
import org.eclipse.rdf4j.model.Resource
import java.time.ZonedDateTime

sealed interface Aff4Model {
  @RdfSubject
  val arn: Resource
}

@RdfModel("aff4:BlockHashes")
data class BlockHashes(
  override val arn: Resource,
  val hash: Hash,
) : Aff4Model

@RdfModel("aff4:ZipVolume")
data class ZipVolume(
  override val arn: Resource,
  val contains: List<Resource>,
  val creationTime: ZonedDateTime,
  @RdfValue("aff4:interface")
  val interfaceType: Resource, // todo this should be an enum?
  val stored: Path,
) : Aff4Model

@RdfModel("aff4:Map")
data class Map(
  override val arn: Resource,
  val blockMapHash: Hash,
  val dependentStream: Resource,
  val mapGapDefaultStream: Resource,
  val mapHash: Hash,
  val mapIdxHash: Hash,
  val mapPathHash: Hash,
  val mapPointHash: Hash,
  val size: Long,
  val stored: Resource,
  val target: Resource,
) : Aff4Model

@RdfModel("aff4:CaseNotes")
data class CaseNotes(
  override val arn: Resource,
  val caseNumber: String,
  val evidenceNumber: String,
  val examiner: String,
  val notes: String,
  val stored: Resource,
  val target: Resource,
  val timestamp: ZonedDateTime,
) : Aff4Model
