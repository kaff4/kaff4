package net.navatwo.kaff4.container

import net.navatwo.kaff4.model.rdf.HashType
import net.navatwo.kaff4.model.rdf.ImageStream
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.streams.image_stream.Aff4ImageStreamSink
import net.navatwo.kaff4.streams.map_stream.Aff4MapStreamSink
import okio.FileSystem
import okio.Timeout

internal interface Aff4StreamSinkFactory {
  fun createMapStreamSink(
    outputFileSystem: FileSystem,
    dataStreamSink: Aff4ImageStreamSink,
    mapStream: MapStream,
    timeout: Timeout,
  ): Aff4MapStreamSink

  fun createImageStreamSink(
    outputFileSystem: FileSystem,
    imageStream: ImageStream,
    blockHashTypes: Collection<HashType>,
    timeout: Timeout,
  ): Aff4ImageStreamSink
}
