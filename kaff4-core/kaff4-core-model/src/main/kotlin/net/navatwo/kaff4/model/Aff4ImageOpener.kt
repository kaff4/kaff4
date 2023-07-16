package net.navatwo.kaff4.model

import net.navatwo.kaff4.api.InternalApi
import okio.FileSystem
import okio.Path

interface Aff4ImageOpener {
  @InternalApi
  fun manualOpen(fileSystem: FileSystem, path: Path): Aff4ImageWithResources

  fun <R> open(fileSystem: FileSystem, path: Path, block: (container: Aff4Image) -> R): R {
    return manualOpen(fileSystem, path).use { wrapper ->
      block(wrapper.aff4Image)
    }
  }

  @InternalApi
  class Aff4ImageWithResources(
    val aff4Image: Aff4Image,
    private val closeable: AutoCloseable,
  ) : AutoCloseable {
    override fun close() {
      aff4Image.close()
      closeable.close()
    }
  }
}
