package net.navatwo.kaff4.io

import com.google.common.collect.HashBiMap
import net.navatwo.kaff4.api.InternalApi
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.FileMetadata
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath

@InternalApi
class Sha256FileSystemFactory {
  fun create(baseDirectory: Path): MappedFileSystem = create(FileSystem.SYSTEM, baseDirectory)

  fun create(baseFileSystem: FileSystem, baseDirectory: Path): MappedFileSystem {
    val writingFileSystem = baseFileSystem.relativeTo(baseDirectory)
    return MappedFileSystem(writingFileSystem)
  }

  class MappedFileSystem internal constructor(
    delegate: RelativeFileSystem,
  ) : ForwardingFileSystem(delegate) {
    val baseDirectory = delegate.rootDirectory

    val mappingsView: HashBiMap<Path, Path>
      get() = HashBiMap.create(mappings)

    private val mappings = HashBiMap.create<Path, Path>()

    internal fun mapExternalPath(path: Path): Path {
      val normalized = path.normalized()
      val sha256 = Buffer().use { it.write(normalized.toString().encodeUtf8()).sha256() }.hex()
      return (sha256.substring(0..1).toPath() / sha256).normalized()
    }

    override fun onPathParameter(path: Path, functionName: String, parameterName: String): Path {
      val normalized = path.normalized()
      val mappedPath = mappings.computeIfAbsent(normalized) {
        val shaPath = mapExternalPath(normalized)

        delegate.createDirectories(shaPath.parent!!)

        shaPath
      }

      return mappedPath
    }

    override fun onPathResult(path: Path, functionName: String): Path {
      return mappings.inverse()[path] ?: error("Can not resolve path not previously mapped: $path")
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
      val normalized = path.normalized()

      if (normalized in mappings) {
        return super.metadataOrNull(path)
      }

      val segments = normalized.segments
      val directoryExists = mappings.keys.any { it.segments.take(segments.size) == segments }
      return if (directoryExists) {
        FileMetadata(
          isRegularFile = false,
          isDirectory = true,
        )
      } else {
        null
      }
    }

    override fun list(dir: Path): List<Path> {
      val normalizedParent = dir.normalized()
      val segments = if (normalizedParent == ".".toPath() || normalizedParent == "".toPath()) {
        listOf()
      } else {
        normalizedParent.segments
      }

      val pathsUnderDir = mappings.keys.filter { mapped ->
        mapped.segments.size > segments.size && mapped.segments.take(segments.size) == segments
      }
      val singleLevelPaths = pathsUnderDir.asSequence()
        .map { it.segments.take(segments.size + 1).fold("".toPath()) { a, b -> a / b } }
        .toSet()

      return singleLevelPaths.toList()
    }

    override fun listRecursively(dir: Path, followSymlinks: Boolean): Sequence<Path> {
      val normalizedDir = dir.normalized()
      val paths = list(normalizedDir)
      return sequence {
        yieldAll(paths)

        for (path in paths) {
          yieldAll(listRecursively(path, followSymlinks))
        }
      }
    }

    /** All directories are synthetic */
    override fun createDirectory(dir: Path, mustCreate: Boolean) = Unit

    override fun delete(path: Path, mustExist: Boolean) {
      super.delete(path, mustExist)

      mappings.remove(path.normalized())
    }
  }
}
