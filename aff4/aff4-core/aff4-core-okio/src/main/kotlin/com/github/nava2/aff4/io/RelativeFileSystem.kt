package com.github.nava2.aff4.io

import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path

class RelativeFileSystem(
  delegate: FileSystem,
  val rootDirectory: Path,
) : ForwardingFileSystem(delegate) {
  override fun onPathParameter(path: Path, functionName: String, parameterName: String): Path {
    return rootDirectory.resolve(path)
  }

  override fun onPathResult(path: Path, functionName: String): Path {
    return path.relativeTo(rootDirectory)
  }

  override fun toString(): String = "Relative($delegate, $rootDirectory)"
}

fun FileSystem.relativeTo(path: Path): RelativeFileSystem {
  return RelativeFileSystem(this, path)
}
