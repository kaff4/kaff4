package com.github.nava2.aff4.model.rdf

import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.rdf.annotations.RdfModel
import com.github.nava2.aff4.model.rdf.annotations.RdfValue
import okio.Path
import org.eclipse.rdf4j.model.Resource
import java.time.ZonedDateTime

sealed interface Aff4RdfBaseModels : Aff4RdfModel

@RdfModel("aff4:BlockHashes")
data class BlockHashes(
  override val arn: Aff4Arn,
  val hash: Hash,
) : Aff4RdfBaseModels {
  val forHashType: HashType = HashType.valueOf(arn.localName.substringAfterLast('.').uppercase())
}

@RdfModel("aff4:ZipVolume")
data class ZipVolume(
  override val arn: Aff4Arn,
  val contains: List<Resource> = listOf(),
  val creationTime: ZonedDateTime,
  @RdfValue("aff4:interface")
  val interfaceType: Resource, // todo this should be an enum?
  val stored: Path,
) : Aff4RdfBaseModels

@RdfModel("aff4:Map")
data class MapStream(
  override val arn: Aff4Arn,
  val dependentStream: Aff4Arn? = null,
  val mapGapDefaultStream: Aff4Arn? = null,
  val mapHash: Hash? = null,
  val blockMapHash: Hash? = null,
  val mapIdxHash: Hash? = null,
  val mapPathHash: Hash? = null,
  val mapPointHash: Hash? = null,
  val size: Long,
  val stored: Aff4Arn? = null,
  val target: Aff4Arn? = null,
) : Aff4RdfBaseModels {
  fun idxPath(containerArn: Aff4Arn) = arn.toAff4Path(stored ?: containerArn) / "idx"
  fun mapPathPath(containerArn: Aff4Arn) = arn.toAff4Path(stored ?: containerArn) / "mapPath"
  fun mapPath(containerArn: Aff4Arn) = arn.toAff4Path(stored ?: containerArn) / "map"
}

@RdfModel("aff4:Image")
data class Image(
  override val arn: Aff4Arn,
  val dataStream: Aff4Arn,
  val size: Long,
) : Aff4RdfBaseModels

@RdfModel("aff4:ImageStream")
data class ImageStream(
  override val arn: Aff4Arn,
  val chunkSize: Int,
  val chunksInSegment: Int,
  val size: Long,
  val compressionMethod: CompressionMethod = CompressionMethod.None,
  @RdfValue("aff4:hash")
  val linearHashes: List<Hash> = listOf(),
  @RdfValue("aff4:imageStreamHash")
  val imageStreamHashes: List<Hash> = listOf(),
  @RdfValue("aff4:imageStreamIndexHash")
  val imageStreamIndexHashes: List<Hash> = listOf(),
  val stored: Aff4Arn? = null,
  val target: Aff4Arn? = null,
  val version: Int = 1,
) : Aff4RdfBaseModels {
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

  fun queryBlockHashes(aff4Model: Aff4Model): List<BlockHashes> {
    return aff4Model.querySubjectStartsWith("$arn/blockhash.", BlockHashes::class)
  }
}

@RdfModel("aff4:CaseNotes")
data class CaseNotes(
  override val arn: Aff4Arn,
  val caseNumber: String? = null,
  val evidenceNumber: String? = null,
  val examiner: String? = null,
  val notes: String? = null,
  val stored: Aff4Arn? = null,
  val target: Aff4Arn? = null,
  val timestamp: ZonedDateTime,
) : Aff4RdfBaseModels

@RdfModel("aff4:CaseDetails")
data class CaseDetails(
  override val arn: Aff4Arn,
  val caseDescription: String? = null,
  val caseName: String? = null,
  val examiner: String? = null,
  val stored: Aff4Arn? = null,
  val target: Aff4Arn? = null,
) : Aff4RdfBaseModels

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
  override val arn: Aff4Arn,
  val endTime: ZonedDateTime,
  val operation: Aff4ImagingOperation,
  val startTime: ZonedDateTime,
  val stored: Aff4Arn? = null,
  val target: Aff4Arn? = null,
  val timeSource: Aff4TimeSource,
) : Aff4RdfBaseModels

@RdfModel("aff4:DiskImage")
data class DiskImage(
  override val arn: Aff4Arn,
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
) : Aff4RdfBaseModels

@RdfModel("aff4:zip_segment")
data class ZipSegment(
  override val arn: Aff4Arn,
  val size: Long,
  @RdfValue("aff4:hash")
  val linearHashes: List<Hash> = listOf(),
  val stored: Aff4Arn,
) : Aff4RdfBaseModels {
  val segmentPath = arn.toAff4Path(stored)
}

@RdfModel("aff4:FileImage")
data class FileImage(
  override val arn: Aff4Arn,
  val originalFileName: Path,
  val size: Long,
  val birthTime: ZonedDateTime? = null,
  val lastAccessed: ZonedDateTime? = null,
  val lastWritten: ZonedDateTime? = null,
  val recordChanged: ZonedDateTime? = null,
) : Aff4RdfBaseModels
