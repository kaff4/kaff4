package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.model.rdf.ImageStream
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import java.util.concurrent.ConcurrentHashMap

internal class Aff4ImageBevies @AssistedInject constructor(
  private val bevyOpener: Bevy.Opener,
  private val aff4BevySourceProviderFactory: Aff4BevySourceProvider.AssistedFactory,
  @Assisted private val imageStream: ImageStream,
  @Assisted internal val bevyChunkCache: BevyChunkCache,
) {
  private val bevies = ConcurrentHashMap<Int, Aff4BevySourceProvider>()

  fun getOrLoadBevy(bevyIndex: Int): Aff4BevySourceProvider = bevies.computeIfAbsent(bevyIndex) { index ->
    val bevy = bevyOpener.open(imageStream, index)
    aff4BevySourceProviderFactory.create(
      imageStreamConfig = imageStream,
      bevyChunkCache = bevyChunkCache,
      bevy = bevy,
    )
  }

  interface Factory {
    fun create(
      imageStream: ImageStream,
      bevyChunkCache: BevyChunkCache = BevyChunkCache(imageStream.chunkSize),
    ): Aff4ImageBevies
  }
}
