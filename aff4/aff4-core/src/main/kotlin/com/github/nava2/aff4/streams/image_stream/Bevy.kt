package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.mapNotNullValues
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.createArn
import com.github.nava2.aff4.model.rdf.toAff4Path
import okio.FileSystem
import okio.Path
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject

private const val BEVY_FILENAME_PADDING_LENGTH = 8

data class Bevy(
  val arn: IRI,
  val index: Int,
  val dataSegment: Path,
  val indexSegment: Path,
  val blockHashes: Map<HashType, Path> = mapOf(),
) {
  companion object {
    internal fun indexId(index: Int) = index.toString().padStart(BEVY_FILENAME_PADDING_LENGTH, '0')

    internal fun computeBlockHashPaths(
      imagePath: Path,
      index: Int,
      blockHashes: Collection<HashType>,
    ): Map<HashType, Path> {
      return blockHashes.associateWith { hashType ->
        imagePath / "${indexId(index)}.${hashType.name.lowercase()}"
      }
    }
  }

  class Factory @Inject internal constructor(
    private val valueFactory: ValueFactory,
  ) {

    fun create(
      imageStream: ImageStream,
      index: Int,
      blockHashes: Collection<HashType>,
    ): Bevy {
      val imageArn = imageStream.arn
      val indexId = indexId(index)
      val arn = valueFactory.createArn("$imageArn/$indexId")
      val imagePath = imageArn.toAff4Path(imageStream.stored!!)

      return Bevy(
        arn = arn,
        index = index,
        dataSegment = imagePath / indexId,
        indexSegment = imagePath / "$indexId.index",
        blockHashes = computeBlockHashPaths(imagePath, index, blockHashes),
      )
    }
  }

  class Opener @Inject internal constructor(
    private val aff4Model: Aff4Model,
    private val bevyFactory: Factory,
    @ForImageRoot private val imageFileSystem: FileSystem,
  ) {
    fun open(
      imageStream: ImageStream,
      index: Int,
    ): Bevy {
      val bevy = bevyFactory.create(
        imageStream = imageStream.copy(stored = imageStream.stored ?: aff4Model.containerArn),
        index = index,
        blockHashes = HashType.values().toSet(),
      )
      return bevy.copy(blockHashes = computeExistingBlockHashPaths(bevy))
    }

    private fun computeExistingBlockHashPaths(
      bevy: Bevy,
    ): Map<HashType, Path> {
      return bevy.blockHashes.mapNotNullValues { (hashType, shortPath) ->
        val longPath = bevy.indexSegment.parent!! / "${indexId(bevy.index)}.blockHash.${hashType.name.lowercase()}"
        when {
          imageFileSystem.exists(shortPath) -> shortPath
          imageFileSystem.exists(longPath) -> longPath
          else -> null
        }
      }
    }
  }
}
