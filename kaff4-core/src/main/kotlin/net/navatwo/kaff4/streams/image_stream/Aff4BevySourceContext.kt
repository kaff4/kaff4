package net.navatwo.kaff4.streams.image_stream

import net.navatwo.kaff4.io.FileSystemPathSourceProvider
import net.navatwo.kaff4.io.sourceProvider
import net.navatwo.kaff4.model.rdf.ImageStream
import okio.FileSystem

internal data class Aff4BevySourceContext(
  val imageBlockHashVerification: ImageBlockHashVerification,
  val bevyIndexReader: BevyIndexReader,
  val bevyChunkCache: BevyChunkCache,
  private val fileSystem: FileSystem,
  val imageStream: ImageStream,
  val bevy: Bevy,
) {
  val uncompressedSize = imageStream.bevySize(bevy.index)

  fun dataSegmentSourceProvider(): FileSystemPathSourceProvider = fileSystem.sourceProvider(bevy.dataSegment)
}
