package net.navatwo.kaff4.model.rdf

import net.navatwo.kaff4.model.Aff4Model
import net.navatwo.kaff4.model.dialect.Aff4LogicalStandardToolDialect.RdfStandardType
import net.navatwo.kaff4.model.querySubjectStartsWith
import net.navatwo.kaff4.model.rdf.annotations.RdfValue
import okio.Path
import org.eclipse.rdf4j.model.Resource
import java.time.ZonedDateTime

sealed interface Aff4RdfBaseModels : Aff4RdfModel

@RdfStandardType("aff4:BlockHashes")
data class BlockHashes(
  override val arn: Aff4Arn,
  val hash: Hash,
) : Aff4RdfBaseModels {
  val forHashType: HashType = HashType.valueOf(arn.localName.substringAfterLast('.').uppercase())
}

@RdfStandardType("aff4:ZipVolume")
data class ZipVolume(
  override val arn: Aff4Arn,
  val contains: Set<Resource> = setOf(),
  val creationTime: ZonedDateTime,
  @RdfValue("aff4:interface")
  val interfaceType: Resource, // todo this should be an enum?
  val stored: Path,
) : Aff4RdfBaseModels

@RdfStandardType("aff4:Map")
data class MapStream(
  override val arn: Aff4Arn,
  @RdfValue("aff4:dependentStream")
  val dependentStreams: Set<Aff4Arn> = setOf(),
  val mapGapDefaultStream: Aff4Arn? = null,
  val mapHash: Hash? = null,
  val blockMapHash: Hash? = null,
  val mapIdxHash: Hash? = null,
  val mapPathHash: Hash? = null,
  val mapPointHash: Hash? = null,
  val size: Long,
  override val stored: Aff4Arn,
  val target: Aff4Arn? = null,
) : Aff4RdfBaseModels, StoredRdfModel {
  val idxPath = arn.toAff4Path(stored) / "idx"
  val mapPathPath = arn.toAff4Path(stored) / "mapPath"
  val mapPath = arn.toAff4Path(stored) / "map"
}

@RdfStandardType("aff4:Image")
data class Image(
  override val arn: Aff4Arn,
  @RdfValue("aff4:dataStream")
  val dataStreams: Set<Aff4Arn>,
  val size: Long,
) : Aff4RdfBaseModels

@RdfStandardType("aff4:ImageStream")
data class ImageStream(
  override val arn: Aff4Arn,
  val chunkSize: Int,
  val chunksInSegment: Int,
  val size: Long,
  val compressionMethod: CompressionMethod = CompressionMethod.None,
  @RdfValue("aff4:hash")
  val linearHashes: Set<Hash> = setOf(),
  @RdfValue("aff4:imageStreamHash")
  val imageStreamHashes: Set<Hash> = setOf(),
  @RdfValue("aff4:imageStreamIndexHash")
  val imageStreamIndexHashes: Set<Hash> = setOf(),
  override val stored: Aff4Arn,
  @RdfValue("aff4:target")
  val targets: Set<Aff4Arn> = setOf(),
  val version: Int = 1,
) : Aff4RdfBaseModels, StoredRdfModel {
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
    return aff4Model.querySubjectStartsWith<BlockHashes>("$arn/blockhash.").toList()
  }
}

@RdfStandardType("aff4:CaseNotes")
data class CaseNotes(
  override val arn: Aff4Arn,
  val caseNumber: String? = null,
  val evidenceNumber: String? = null,
  val examiner: String? = null,
  val notes: String? = null,
  val stored: Set<Aff4Arn>,
  val target: Aff4Arn? = null,
  val timestamp: ZonedDateTime,
) : Aff4RdfBaseModels

@RdfStandardType("aff4:CaseDetails")
data class CaseDetails(
  override val arn: Aff4Arn,
  val caseDescription: String? = null,
  val caseName: String? = null,
  val examiner: String? = null,
  val stored: Set<Aff4Arn>,
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

@RdfStandardType("aff4:TimeStamps")
data class TimeStamps(
  override val arn: Aff4Arn,
  val endTime: ZonedDateTime,
  val operation: Aff4ImagingOperation,
  val startTime: ZonedDateTime,
  val stored: Set<Aff4Arn>,
  val target: Aff4Arn? = null,
  val timeSource: Aff4TimeSource,
) : Aff4RdfBaseModels

@RdfStandardType("aff4:DiskImage")
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

@RdfStandardType("aff4:ZipSegment")
data class ZipSegment(
  override val arn: Aff4Arn,
  val size: Long,
  @RdfValue("aff4:hash")
  val linearHashes: Set<Hash> = setOf(),
  override val stored: Aff4Arn,
) : Aff4RdfBaseModels, StoredRdfModel {
  val segmentPath = arn.toAff4Path(stored)
}

@RdfStandardType("aff4:FileImage")
data class FileImage(
  override val arn: Aff4Arn,
  val originalFileName: Path,
  val size: Long,
  val birthTime: ZonedDateTime? = null,
  val lastAccessed: ZonedDateTime? = null,
  val lastWritten: ZonedDateTime? = null,
  val recordChanged: ZonedDateTime? = null,
) : Aff4RdfBaseModels
