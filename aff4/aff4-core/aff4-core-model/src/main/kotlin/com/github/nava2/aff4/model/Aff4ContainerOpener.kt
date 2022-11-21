package com.github.nava2.aff4.model

import okio.FileSystem
import okio.Path

interface Aff4ContainerOpener {
  fun manualOpen(fileSystem: FileSystem, path: Path): Aff4ContainerWithResources

  fun <R> open(fileSystem: FileSystem, path: Path, block: (container: Aff4Container) -> R): R {
    return manualOpen(fileSystem, path).use { wrapper ->
      block(wrapper.aff4Container)
    }
  }

  class Aff4ContainerWithResources(
    val aff4Container: Aff4Container,
    private val closeable: AutoCloseable,
  ) : AutoCloseable {
    override fun close() {
      aff4Container.close()
      closeable.close()
    }
  }
}
