package com.github.nava2.aff4.model

import okio.FileSystem
import okio.Path

interface Aff4ContainerOpener {
  fun open(fileSystem: FileSystem, path: Path): Aff4Container
}
