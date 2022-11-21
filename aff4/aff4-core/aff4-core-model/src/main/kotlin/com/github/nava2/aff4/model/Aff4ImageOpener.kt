package com.github.nava2.aff4.model

import okio.FileSystem
import okio.Path

interface Aff4ImageOpener {
  fun manualOpen(fileSystem: FileSystem, path: Path): Aff4ContainerWithResources

  fun <R> open(fileSystem: FileSystem, path: Path, block: (container: Aff4Image) -> R): R {
    return manualOpen(fileSystem, path).use { wrapper ->
      block(wrapper.aff4Image)
    }
  }

  class Aff4ContainerWithResources(
    val aff4Image: Aff4Image,
    private val closeable: AutoCloseable,
  ) : AutoCloseable {
    override fun close() {
      aff4Image.close()
      closeable.close()
    }
  }
}
