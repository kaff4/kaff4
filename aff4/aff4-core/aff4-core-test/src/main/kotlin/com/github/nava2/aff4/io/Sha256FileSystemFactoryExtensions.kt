package com.github.nava2.aff4.io

import com.github.nava2.aff4.io.Sha256FileSystemFactory.MappedFileSystem
import okio.Path.Companion.toOkioPath
import java.nio.file.Path

fun Sha256FileSystemFactory.create(path: Path): MappedFileSystem {
  return create(path.toOkioPath())
}
