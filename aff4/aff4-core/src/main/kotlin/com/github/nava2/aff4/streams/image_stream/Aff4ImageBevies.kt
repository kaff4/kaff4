package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.meta.rdf.model.ImageStream
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject

internal class Aff4ImageBevies @AssistedInject constructor(
  private val bevyIndexReaderFactory: BevyIndexReader.Factory,
  private val bevyFactory: Bevy.Factory,
  private val aff4BevyFactory: Aff4Bevy.AssistedFactory,
  @Assisted private val imageStreamConfig: ImageStream,
  @Assisted internal val bevyChunkCache: BevyChunkCache,
) : AutoCloseable {

  private val bevies = mutableMapOf<Int, Aff4Bevy>()

  fun getOrLoadBevy(bevyIndex: Int): Aff4Bevy = bevies.getOrPut(bevyIndex) {
    val bevy = bevyFactory.create(imageStreamConfig, bevyIndex)
    aff4BevyFactory.create(
      imageStreamConfig = imageStreamConfig,
      bevyIndexReader = bevyIndexReaderFactory.create(imageStreamConfig, bevy),
      bevyChunkCache = bevyChunkCache,
      bevy = bevy,
    )
  }

  override fun close() {
    val valuesToClose = bevies.values.toList()
    bevies.clear()

    for (aff4Bevy in valuesToClose) {
      aff4Bevy.close()
    }
  }

  interface Factory {
    fun create(
      imageStreamConfig: ImageStream,
      bevyChunkCache: BevyChunkCache = BevyChunkCache(imageStreamConfig.chunkSize),
    ): Aff4ImageBevies
  }
}
