package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.meta.rdf.model.HashType
import com.github.nava2.aff4.meta.rdf.model.ImageStream
import com.github.nava2.aff4.meta.rdf.toAff4Path
import com.github.nava2.aff4.model.Aff4Model
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
      val imagePath = imageArn.toAff4Path()
      return Bevy(
        arn = arn,
        index = index,
        dataSegment = imagePath / indexId,
        indexSegment = imagePath / "$indexId.index",
        blockHashes = computeBlockHashPaths(imageStream, arn, indexId)
      )
    }

    private fun computeBlockHashPaths(
      imageStream: ImageStream,
      bevyArn: IRI,
      indexId: String
    ): Map<HashType, Path> {
      val blockHashes = imageStream.queryBlockHashes(aff4Model)
      val imagePath = imageStream.arn.toAff4Path()

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
