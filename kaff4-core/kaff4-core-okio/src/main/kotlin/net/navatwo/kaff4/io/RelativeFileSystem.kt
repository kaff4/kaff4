package net.navatwo.kaff4.io

import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath

class RelativeFileSystem(
  delegate: FileSystem,
  rootDirectory: Path,
) : ForwardingFileSystem(delegate) {

  val rootDirectory: Path = rootDirectory.normalized()

  override fun onPathParameter(path: Path, functionName: String, parameterName: String): Path {
    val superResolved = super.onPathParameter(path, functionName, parameterName).trimLeading()
    return rootDirectory.resolve(superResolved, true)
  }

  override fun onPathResult(path: Path, functionName: String): Path {
    val superResolved = super.onPathResult(path, functionName).trimLeading()
    return superResolved.relativeTo(rootDirectory)
  }

  override fun toString(): String = "Relative($delegate, $rootDirectory)"

  private fun Path.trimLeading(): Path {
    if (rootDirectory.isAbsolute) return this

    return toString()
      .trimStart('/', '\\')
      .toPath(normalize = true)
  }
}

fun FileSystem.relativeTo(path: Path): RelativeFileSystem {
  return RelativeFileSystem(this, path)
}
