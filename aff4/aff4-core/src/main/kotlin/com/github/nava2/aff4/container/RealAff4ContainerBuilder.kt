package com.github.nava2.aff4.container

import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.Aff4Schema
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.rdf.RdfConnectionScoping
import com.github.nava2.aff4.rdf.ScopedConnection
import com.github.nava2.aff4.rdf.io.RdfModelSerializer
import com.github.nava2.aff4.rdf.schema.RdfSchema
import com.github.nava2.aff4.rdf.schema.XsdSchema
import com.github.nava2.aff4.streams.Aff4Sink
import com.github.nava2.aff4.streams.image_stream.Aff4ImageStreamSink
import com.github.nava2.aff4.streams.map_stream.Aff4MapStreamSink
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Timeout
import okio.buffer
import okio.openZip
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class RealAff4ContainerBuilder @AssistedInject internal constructor(
  private val streamSinkFactory: Aff4StreamSinkFactory,
  private val rdfConnectionScoping: RdfConnectionScoping,
  @Assisted private val temporaryFileSystem: FileSystem,
  @Assisted override val arn: Aff4Arn,
  @Assisted override val defaultTimeout: Timeout,
) : Aff4ContainerBuilder {
  init {
    setupContainerNamespaces()
  }

  private val sinks = ConcurrentHashMap<Aff4Arn, Aff4Sink>()

  @Volatile
  private var closed = false

  override fun createImageStream(
    imageStream: ImageStream,
    blockHashTypes: Collection<HashType>,
    timeout: Timeout,
  ): Aff4ImageStreamSink {
    check(!closed) { "closed" }

    val sink = sinks.computeIfAbsent(imageStream.arn) {
      streamSinkFactory.createImageStreamSink(temporaryFileSystem, imageStream, blockHashTypes, timeout)
    }
    return sink as Aff4ImageStreamSink
  }

  override fun createMapStream(
    mapStream: MapStream,
    dataStreamSink: Aff4Sink,
    timeout: Timeout,
  ): Aff4MapStreamSink {
    check(!closed) { "closed" }

    val sink = sinks.computeIfAbsent(mapStream.arn) {
      streamSinkFactory.createMapStreamSink(
        outputFileSystem = temporaryFileSystem,
        dataStreamSink = dataStreamSink as Aff4ImageStreamSink,
        mapStream = mapStream,
        timeout = timeout,
      )
    }

    return sink as Aff4MapStreamSink
  }

  override fun close() {
    if (closed) return
    synchronized(this) {
      if (closed) return
      closed = true
    }

    for (sink in sinks.values) {
      sink.close()
    }

    sinks.clear()
  }

  override fun buildIntoDirectory(fileSystem: FileSystem, path: Path) {
    val outputFileSystem = fileSystem.relativeTo(path)
    outputFileSystem.createDirectories("".toPath())

    exportInto(outputFileSystem)
  }

  override fun buildIntoZip(path: Path) {
    val fileSystem = FileSystem.SYSTEM.openZip(path)

    exportInto(fileSystem)
  }

  private fun exportInto(outputFileSystem: FileSystem) {
    rdfConnectionScoping.scoped { containerMetaWriter: ContainerMetaWriter, serializer: ConnectionSerializer ->
      serializer.dumpToConnection(sinks.values.map { it.model })

      containerMetaWriter.write(temporaryFileSystem, arn)
    }

    val pathsAndMetadata = temporaryFileSystem.listRecursively(".".toPath())
      .map { it to temporaryFileSystem.metadata(it) }

    for ((path, metadata) in pathsAndMetadata) {
      when {
        metadata.isDirectory -> outputFileSystem.createDirectories(path)
        metadata.isRegularFile -> {
          outputFileSystem.write(path, mustCreate = true) {
            temporaryFileSystem.source(path).buffer().use { source ->
              writeAll(source)
            }
          }
        }
        else -> error("Invalid metadata: $path -> $metadata")
      }
    }
  }

  private fun setupContainerNamespaces() {
    rdfConnectionScoping.scoped { scopedConnection: ScopedConnection ->
      scopedConnection.mutable.apply {
        setNamespace("", arn.stringValue())
        setNamespace("rdf", RdfSchema.SCHEMA)
        setNamespace("xsd", XsdSchema.SCHEMA)
        setNamespace("aff4", Aff4Schema.SCHEMA)
      }
    }
  }
}

private class ConnectionSerializer @Inject constructor(
  private val connection: ScopedConnection,
  private val serializer: RdfModelSerializer,
) {
  fun dumpToConnection(objects: Collection<Any>) {
    connection.mutable.apply {
      for (obj in objects) {
        add(serializer.serialize(obj))
      }
    }
  }
}
