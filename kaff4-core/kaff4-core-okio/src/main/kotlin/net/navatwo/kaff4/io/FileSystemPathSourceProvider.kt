package net.navatwo.kaff4.io

import net.navatwo.kaff4.api.InternalApi
import okio.FileSystem
import okio.Path
import okio.Timeout

@InternalApi
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

    return fileSystem.source(path).asKAff4().buffer().applyAndCloseOnThrow {
      if (position != 0L) {
        skipFully(position)
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
