package net.navatwo.kaff4.io

import net.navatwo.kaff4.io.Sha256FileSystemFactory.MappedFileSystem
import okio.Path.Companion.toOkioPath
import java.nio.file.Path

fun Sha256FileSystemFactory.create(path: Path): MappedFileSystem {
  return create(path.toOkioPath())
}
