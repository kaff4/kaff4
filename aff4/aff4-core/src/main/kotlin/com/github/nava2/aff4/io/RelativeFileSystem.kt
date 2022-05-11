package com.github.nava2.aff4.io

import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path

class RelativeFileSystem(
  delegate: FileSystem,
  val rootDirectory: Path,
) : ForwardingFileSystem(delegate) {
  override fun onPathParameter(path: Path, functionName: String, parameterName: String): Path {
    val resolved = rootDirectory.resolve(path)
    require(resolved.parent == rootDirectory) {
      "Can not escape relative file system: $path"
    }
    return resolved
  }

  override fun onPathResult(path: Path, functionName: String): Path {
    return path.relativeTo(rootDirectory)
  }
}

fun FileSystem.relativeTo(path: Path): FileSystem {
  return RelativeFileSystem(this, path)
}
