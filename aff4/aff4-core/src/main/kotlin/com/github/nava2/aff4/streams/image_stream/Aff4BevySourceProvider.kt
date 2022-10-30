package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.io.SourceProvider
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.rdf.ImageStream
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.Closeable
import okio.FileSystem
import okio.Source
import okio.Timeout
import javax.inject.Provider

internal class Aff4BevySourceProvider @AssistedInject constructor(
  imageBlockHashVerification: ImageBlockHashVerification,
  bevyIndexReaderFactory: BevyIndexReader.Factory,
  @ForImageRoot fileSystemProvider: Provider<FileSystem>,
  @Assisted bevyChunkCache: BevyChunkCache,
  @Assisted imageStream: ImageStream,
  @Assisted val bevy: Bevy,
) : SourceProvider<Source>, Closeable {
  private val bevyIndexReader: BevyIndexReader = bevyIndexReaderFactory.create(imageStream, bevy)

  private val bevySourceContext = Aff4BevySourceContext(
    imageBlockHashVerification = imageBlockHashVerification,
    bevyIndexReader = bevyIndexReader,
    bevyChunkCache = bevyChunkCache,
    fileSystem = fileSystemProvider.get(),
    imageStream = imageStream,
    bevy = bevy,
  )

  val uncompressedSize = imageStream.bevySize(bevy.index)

  override fun source(position: Long, timeout: Timeout): Source {
    return Aff4BevySource(
      context = bevySourceContext,
      position = position,
      timeout = timeout,
    )
  }

  override fun close() {
    bevyIndexReader.close()
  }

  interface AssistedFactory {
    fun create(
      imageStreamConfig: ImageStream,
      bevyChunkCache: BevyChunkCache,
      bevy: Bevy,
    ): Aff4BevySourceProvider
  }
}
