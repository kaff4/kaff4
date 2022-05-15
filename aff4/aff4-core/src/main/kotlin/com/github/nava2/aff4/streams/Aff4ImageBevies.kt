package com.github.nava2.aff4.streams

import com.github.nava2.aff4.meta.rdf.model.ImageStream
import okio.FileSystem

internal class Aff4ImageBevies(
  private val bevyFactory: Bevy.Factory,
  private val fileSystem: FileSystem,
  private val imageStreamConfig: ImageStream,
  internal val bevyChunkCache: BevyChunkCache = BevyChunkCache(imageStreamConfig.chunkSize),
) : AutoCloseable {

  private val bevyIndexReader: BevyIndexReader = BevyIndexReader(fileSystem, imageStreamConfig)
  private val bevies = mutableMapOf<Int, Aff4Bevy>()

  fun getOrLoadBevy(bevyIndex: Int): Aff4Bevy = bevies.getOrPut(bevyIndex) {
    val bevy = bevyFactory.create(imageStreamConfig.arn, bevyIndex)
    Aff4Bevy(fileSystem, imageStreamConfig, bevyIndexReader, bevyChunkCache, bevy)
  }

  override fun close() {
    val valuesToClose = bevies.values.toList()
    bevies.clear()

    bevyIndexReader.close()

    for (aff4Bevy in valuesToClose) {
      aff4Bevy.close()
    }
  }
}
