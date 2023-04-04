package net.navatwo.kaff4.io

import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.Timeout
import okio.buffer

class FileSystemPathSourceProvider internal constructor(
  private val fileSystem: FileSystem,
  val path: Path,
) : SourceProvider<BufferedSource> {
  init {
    require(fileSystem.exists(path)) {
      "File does not exist: $path"
    }
  }

  override fun source(position: Long, timeout: Timeout): BufferedSource {
    require(position >= 0L) { "position < 0" }
    require(timeout == Timeout.NONE) { "Timeouts are not supported for file system sources. " }

    return fileSystem.source(path).buffer().applyAndCloseOnThrow {
      if (position != 0L) {
        skip(position)
      }
    }
  }

  override fun toString(): String {
    return "${javaClass.simpleName}($path)"
  }
}

fun FileSystem.sourceProvider(path: Path): FileSystemPathSourceProvider {
  return FileSystemPathSourceProvider(this, path)
}
