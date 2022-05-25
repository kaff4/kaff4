package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.model.rdf.ImageStream
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject

internal class Aff4ImageBevies @AssistedInject constructor(
  private val bevyIndexReaderFactory: BevyIndexReader.Factory,
  private val bevyOpener: Bevy.Opener,
  private val aff4BevySourceProviderFactory: Aff4BevySourceProvider.AssistedFactory,
  @Assisted private val imageStreamConfig: ImageStream,
  @Assisted internal val bevyChunkCache: BevyChunkCache,
) : AutoCloseable {

  @Volatile
  private var closed = false

  private val bevies = mutableMapOf<Int, Aff4BevySourceProvider>()

  fun getOrLoadBevy(bevyIndex: Int): Aff4BevySourceProvider {
    check(!closed) { "closed" }

    return bevies.getOrPut(bevyIndex) {
      val bevy = bevyOpener.open(imageStreamConfig, bevyIndex)
      aff4BevySourceProviderFactory.create(
        imageStreamConfig = imageStreamConfig,
        bevyIndexReader = bevyIndexReaderFactory.create(imageStreamConfig, bevy),
        bevyChunkCache = bevyChunkCache,
        bevy = bevy,
      )
    }
  }

  override fun close() {
    if (closed) return
    synchronized(this) {
      if (closed) return
      closed = true
    }

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
