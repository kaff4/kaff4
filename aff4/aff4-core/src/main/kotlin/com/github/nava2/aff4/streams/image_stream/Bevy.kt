package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.toAff4Path
import okio.FileSystem
import okio.Path
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject

private const val BEVY_FILENAME_PADDING_LENGTH = 8

internal data class Bevy(
  val arn: IRI,
  val index: Int,
  val dataSegment: Path,
  val indexSegment: Path,
  val blockHashes: Map<HashType, Path>,
) {
  class Factory @Inject constructor(
    private val aff4Model: Aff4Model,
    private val valueFactory: ValueFactory,
    @ForImageRoot private val imageFileSystem: FileSystem,
  ) {
    fun create(
      imageStream: ImageStream,
      index: Int,
    ): Bevy {
      val imageArn = imageStream.arn
      val indexId = indexId(index)
      val arn = valueFactory.createIRI("$imageArn/$indexId")
      val imagePath = imageArn.toAff4Path(imageStream.stored ?: aff4Model.containerArn)
      return Bevy(
        arn = arn,
        index = index,
        dataSegment = imagePath / indexId,
        indexSegment = imagePath / "$indexId.index",
        blockHashes = computeBlockHashPaths(imageStream, imagePath, arn, indexId)
      )
    }

    private fun computeBlockHashPaths(
      imageStream: ImageStream,
      imagePath: Path,
      bevyArn: IRI,
      indexId: String
    ): Map<HashType, Path> {
      val blockHashes = imageStream.queryBlockHashes(aff4Model)

      return blockHashes.associate { hash ->
        val hashTypeExtension = hash.arn.localName.substringAfterLast('.').uppercase()
        val hashType = HashType.valueOf(hashTypeExtension)

        val shortPath = imagePath / "$indexId.${hashType.name.lowercase()}"
        val longPath = imagePath / "$indexId.blockHash.${hashType.name.lowercase()}"
        when {
          imageFileSystem.exists(shortPath) -> hashType to shortPath
          imageFileSystem.exists(longPath) -> hashType to longPath
          else -> error("BlockHash $hashType defined but no blockHash found: $bevyArn")
        }
      }
    }

    private fun indexId(index: Int) = index.toString().padStart(BEVY_FILENAME_PADDING_LENGTH, '0')
  }
}
