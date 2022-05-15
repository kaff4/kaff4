package com.github.nava2.aff4.meta.rdf.model

import com.github.nava2.aff4.meta.rdf.io.RdfModel
import com.github.nava2.aff4.meta.rdf.io.RdfSubject
import com.github.nava2.aff4.meta.rdf.io.RdfValue
import okio.Path
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import java.time.ZonedDateTime

sealed interface Aff4RdfModel {
  @RdfSubject
  val arn: IRI
}

@RdfModel("aff4:BlockHashes")
data class BlockHashes(
  override val arn: IRI,
  val hash: Hash,
) : Aff4RdfModel

@RdfModel("aff4:ZipVolume")
data class ZipVolume(
  override val arn: IRI,
  val contains: List<Resource>,
  val creationTime: ZonedDateTime,
  @RdfValue("aff4:interface")
  val interfaceType: Resource, // todo this should be an enum?
  val stored: Path,
) : Aff4RdfModel

@RdfModel("aff4:Map")
data class Map(
  override val arn: IRI,
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
) : Aff4RdfModel

@RdfModel("aff4:CaseNotes")
data class CaseNotes(
  override val arn: IRI,
  val caseNumber: String,
  val evidenceNumber: String,
  val examiner: String,
  val notes: String,
  val stored: Resource,
  val target: Resource,
  val timestamp: ZonedDateTime,
) : Aff4RdfModel

@RdfModel("aff4:CaseDetails")
data class CaseDetails(
  override val arn: IRI,
  val caseDescription: String,
  val caseName: String,
  val examiner: String,
  val stored: Resource,
  val target: Resource,
) : Aff4RdfModel

enum class Aff4ImagingOperation {
  CAPTURE,
  ;
}

enum class Aff4TimeSource {
  SINK,
  ;
}

@RdfModel("aff4:TimeStamps")
data class TimeStamps(
  override val arn: IRI,
  val endTime: ZonedDateTime,
  val operation: Aff4ImagingOperation,
  val startTime: ZonedDateTime,
  val stored: Resource,
  val target: Resource,
  val timeSource: Aff4TimeSource,
) : Aff4RdfModel

@RdfModel("aff4:Image")
data class Image(
  override val arn: IRI,
  val dataStream: Resource,
  val size: Long,
) : Aff4RdfModel

@RdfModel("aff4:ImageStream")
data class ImageStream(
  override val arn: IRI,
  val chunkSize: Int,
  val chunksInSegment: Int,
  val size: Long,
  val compressionMethod: CompressionMethod,
  val hash: List<Hash>,
  val imageStreamHash: Hash,
  val imageStreamIndexHash: Hash,
  val stored: Resource,
  val target: Resource,
  val version: Int,
) : Aff4RdfModel {
  /** Maximum uncompressed size a bevy stores */
  val bevyMaxSize: Long = chunkSize.toLong() * chunksInSegment

  /** How many bevies are there in [this] */
  val bevyCount: Int = size.floorDiv(bevyMaxSize).toInt() + 1

  /** The index of the last bevy */
  val lastBevyIndex: Int = size.floorDiv(bevyMaxSize).toInt()

  /** Size of the last bevy as it is typically not full */
  val lastBevySize: Long = size - (bevyCount - 1) * bevyMaxSize

  /** Compute the bevy size of [index] via constant-time math */
  fun bevySize(index: Int): Long = if (index != lastBevyIndex) {
    bevyMaxSize
  } else {
    lastBevySize
  }
}

@RdfModel("aff4:DiskImage")
data class DiskImage(
  override val arn: IRI,
  val size: Long,
  val blockSize: Int,
  val sectorCount: Long,
  val acquisitionCompletionState: String,
  val diskPartitionTableType: String,
  val diskNumberOfPartitions: Int,
  val diskDeviceName: String,
  val diskDeviceRole: Int,
  val diskDeviceType: String,
  val diskFirmware: String,
  val diskInterfaceType: String,
  val diskMake: String,
  val diskModel: String,
  val diskSerial: String,
) : Aff4RdfModel
