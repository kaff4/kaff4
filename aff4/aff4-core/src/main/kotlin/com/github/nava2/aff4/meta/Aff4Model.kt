package com.github.nava2.aff4.meta

import com.github.nava2.aff4.meta.rdf.io.RdfModel
import com.github.nava2.aff4.meta.rdf.io.RdfSubject
import com.github.nava2.aff4.meta.rdf.io.RdfValue
import okio.Path
import org.eclipse.rdf4j.model.Resource
import java.time.ZonedDateTime

sealed interface Aff4Model

@RdfModel("aff4:BlockHashes")
data class BlockHashes(
  @RdfSubject val arn: Resource,
  val hash: Hash,
) : Aff4Model

@RdfModel("aff4:ZipVolume")
data class ZipVolume(
  @RdfSubject val arn: Resource,
  val contains: List<Resource>,
  val creationTime: ZonedDateTime,
  @RdfValue("aff4:interface")
  val interfaceType: Resource, // todo this should be an enum?
  val stored: Path,
) : Aff4Model
