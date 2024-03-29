package net.navatwo.kaff4.io

import okio.FileSystem
import okio.Path.Companion.toOkioPath

fun FileSystem.relativeTo(path: java.nio.file.Path): RelativeFileSystem = relativeTo(path.toOkioPath())
