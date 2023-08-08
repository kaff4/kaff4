package net.navatwo.kaff4.streams.image_stream

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import net.navatwo.kaff4.container.ContainerDataFileSystemProvider
import net.navatwo.kaff4.io.Source
import net.navatwo.kaff4.io.SourceProvider
import net.navatwo.kaff4.model.rdf.ImageStream
import okio.Closeable
import okio.Timeout

internal class Aff4BevySourceProvider @AssistedInject constructor(
  imageBlockHashVerification: ImageBlockHashVerification,
  bevyIndexReaderFactory: BevyIndexReader.Factory,
  containerDataFileSystemProvider: ContainerDataFileSystemProvider,
  @Assisted bevyChunkCache: BevyChunkCache,
  @Assisted imageStream: ImageStream,
  @Assisted val bevy: Bevy,
) : SourceProvider<Source>, Closeable {
  @Volatile
  private var closed = false

  private val bevyIndexReader: BevyIndexReader = bevyIndexReaderFactory.create(imageStream, bevy)

  private val bevySourceContext = Aff4BevySourceContext(
    imageBlockHashVerification = imageBlockHashVerification,
    bevyIndexReader = bevyIndexReader,
    bevyChunkCache = bevyChunkCache,
    fileSystem = containerDataFileSystemProvider.get(imageStream),
    imageStream = imageStream,
    bevy = bevy,
  )

  val uncompressedSize = imageStream.bevySize(bevy.index)

  override fun source(position: Long, timeout: Timeout): Source {
    check(!closed) { "closed" }

    return Aff4BevySource(
      context = bevySourceContext,
      position = position,
      timeout = timeout,
    )
  }

  override fun close() {
    if (closed) return

    closed = true
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
