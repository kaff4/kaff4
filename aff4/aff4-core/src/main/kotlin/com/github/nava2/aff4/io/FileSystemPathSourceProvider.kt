package com.github.nava2.aff4.io

import okio.FileSystem
import okio.Path
import okio.Source

class FileSystemPathSourceProvider internal constructor(
  private val fileSystem: FileSystem,
  val path: Path,
) : SourceProvider<Source> {
  init {
    require(fileSystem.exists(path)) {
      "File does not exist: $path"
    }
  }

  override fun get(): Source = fileSystem.source(path)

  override fun toString(): String {
    return "${javaClass.simpleName}($path)"
  }
}

fun FileSystem.sourceProvider(path: Path): FileSystemPathSourceProvider {
  return FileSystemPathSourceProvider(this, path)
}
