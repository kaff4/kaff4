package net.navatwo.kaff4.container

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import net.navatwo.kaff4.io.relativeTo
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.Aff4Schema
import net.navatwo.kaff4.model.rdf.HashType
import net.navatwo.kaff4.model.rdf.ImageStream
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.rdf.MutableRdfConnection
import net.navatwo.kaff4.rdf.RdfExecutor
import net.navatwo.kaff4.rdf.io.RdfModelSerializer
import net.navatwo.kaff4.rdf.schema.RdfSchema
import net.navatwo.kaff4.rdf.schema.XsdSchema
import net.navatwo.kaff4.streams.Aff4Sink
import net.navatwo.kaff4.streams.image_stream.Aff4ImageStreamSink
import net.navatwo.kaff4.streams.map_stream.Aff4MapStreamSink
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Timeout
import okio.buffer
import okio.openZip
import java.util.concurrent.ConcurrentHashMap

internal class RealAff4ContainerBuilder @AssistedInject internal constructor(
  private val streamSinkFactory: Aff4StreamSinkFactory,
  private val rdfExecutor: RdfExecutor,
  private val containerMetaWriter: ContainerMetaWriter,
  rdfModelSerializerFactory: RdfModelSerializer.Factory,
  @Assisted context: Aff4ContainerBuilder.Context,
) : Aff4ContainerBuilder {

  private val rdfModelSerializer = rdfModelSerializerFactory.create(context.toolDialect)
  private val temporaryFileSystem: FileSystem = context.temporaryFileSystem
  override val containerArn: Aff4Arn = context.arn
  override val defaultTimeout: Timeout = context.defaultTimeout

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
      streamSinkFactory.createImageStreamSink(
        outputFileSystem = temporaryFileSystem,
        imageStream = imageStream.copy(stored = containerArn),
        blockHashTypes = blockHashTypes,
        timeout = timeout,
      )
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
        mapStream = mapStream.copy(stored = containerArn),
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
    outputFileSystem.createDirectories(".".toPath())

    exportInto(outputFileSystem)
  }

  override fun buildIntoZip(path: Path) {
    val fileSystem = FileSystem.SYSTEM.openZip(path)

    exportInto(fileSystem)
  }

  private fun exportInto(outputFileSystem: FileSystem) {
    rdfExecutor.withReadWriteSession { connection ->
      rdfModelSerializer.serializeAllToConnection(connection, sinks.values.map { it.model })

      containerMetaWriter.write(connection, temporaryFileSystem, containerArn)
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
    rdfExecutor.withReadWriteSession { connection ->
      connection.apply {
        setNamespace("", containerArn.stringValue())
        setNamespace("rdf", RdfSchema.SCHEMA)
        setNamespace("xsd", XsdSchema.SCHEMA)
        setNamespace("aff4", Aff4Schema.SCHEMA)
      }
    }
  }
}

private fun RdfModelSerializer.serializeAllToConnection(connection: MutableRdfConnection, objects: Collection<Any>) {
  connection.apply {
    for (obj in objects) {
      add(serialize(connection, obj))
    }
  }
}
