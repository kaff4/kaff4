package net.navatwo.kaff4.container

import net.navatwo.kaff4.io.SeekableSink
import net.navatwo.kaff4.io.SizedSink
import net.navatwo.kaff4.model.dialect.ToolDialect
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.HashType
import net.navatwo.kaff4.model.rdf.ImageStream
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.streams.Aff4Sink
import okio.FileSystem
import okio.Path
import okio.Timeout
import java.io.Closeable

interface Aff4ContainerBuilder : Closeable {
  val containerArn: Aff4Arn

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

  data class Context(
    val temporaryFileSystem: FileSystem,
    val arn: Aff4Arn,
    val toolDialect: ToolDialect,
    val defaultTimeout: Timeout = Timeout.NONE,
  )

  interface Factory {
    fun create(context: Context): Aff4ContainerBuilder
  }
}
