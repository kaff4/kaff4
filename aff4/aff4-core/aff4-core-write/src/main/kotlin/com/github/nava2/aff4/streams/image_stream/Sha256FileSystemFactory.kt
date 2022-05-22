package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.io.relativeTo
import com.google.common.collect.HashBiMap
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath

class Sha256FileSystemFactory {
  fun create(baseDirectory: Path): FileSystem {
    val writingFileSystem = FileSystem.SYSTEM.relativeTo(baseDirectory)
    return MappedFileSystem(writingFileSystem)
  }

  private class MappedFileSystem(delegate: FileSystem) : ForwardingFileSystem(delegate) {
    private val mappings = HashBiMap.create<Path, Path>()

    override fun onPathParameter(path: Path, functionName: String, parameterName: String): Path {
      val normalized = path.normalized()
      val mappedPath = mappings.computeIfAbsent(normalized) {
        val sha256 = Buffer().use { it.write(normalized.toString().encodeUtf8()).sha256() }.hex()
        val shaPath = (sha256.substring(0..1).toPath() / sha256).normalized()

        delegate.createDirectories(shaPath.parent!!)

        shaPath
      }

      return mappedPath
    }

    override fun onPathResult(path: Path, functionName: String): Path {
      return mappings.inverse()[path] ?: error("Can not resolve path not previously mapped: $path")
    }

    override fun list(dir: Path): List<Path> {
      val normalizedParent = dir.normalized()
      return mappings.keys.filter { it.parent == normalizedParent }
    }

    /** All directories are synthetic */
    override fun createDirectory(dir: Path, mustCreate: Boolean) = Unit

    override fun delete(path: Path, mustExist: Boolean) {
      super.delete(path, mustExist)

      mappings.remove(path.normalized())
    }
  }
}
