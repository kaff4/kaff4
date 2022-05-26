package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.io.FileSystemPathSourceProvider
import com.github.nava2.aff4.io.sourceProvider
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.rdf.ImageStream
import okio.FileSystem

internal data class Aff4BevySourceContext(
  val imageBlockHashVerification: ImageBlockHashVerification,
  val bevyIndexReader: BevyIndexReader,
  val bevyChunkCache: BevyChunkCache,
  @ForImageRoot private val fileSystem: FileSystem,
  val imageStream: ImageStream,
  val bevy: Bevy,
) {
  val uncompressedSize = imageStream.bevySize(bevy.index)

  fun dataSegmentSourceProvider(): FileSystemPathSourceProvider = fileSystem.sourceProvider(bevy.dataSegment)
}
