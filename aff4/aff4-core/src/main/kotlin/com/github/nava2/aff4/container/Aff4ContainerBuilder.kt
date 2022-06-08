package com.github.nava2.aff4.container

import com.github.nava2.aff4.io.SeekableSink
import com.github.nava2.aff4.io.SizedSink
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.streams.Aff4Sink
import okio.FileSystem
import okio.Path
import okio.Timeout
import java.io.Closeable

interface Aff4ContainerBuilder : Closeable {
  val arn: Aff4Arn

  val defaultTimeout: Timeout

  fun createImageStream(
    imageStream: ImageStream,
    blockHashTypes: Collection<HashType>,
    timeout: Timeout = defaultTimeout,
  ): SizedSink

  fun createMapStream(
    mapStream: MapStream,
    dataStreamSink: Aff4Sink,
    timeout: Timeout = defaultTimeout,
  ): SeekableSink

  fun buildIntoDirectory(fileSystem: FileSystem, path: Path)

  fun buildIntoZip(path: Path)

  interface Factory {
    fun create(
      temporaryFileSystem: FileSystem,
      arn: Aff4Arn,
      defaultTimeout: Timeout = Timeout.NONE,
    ): Aff4ContainerBuilder
  }
}
