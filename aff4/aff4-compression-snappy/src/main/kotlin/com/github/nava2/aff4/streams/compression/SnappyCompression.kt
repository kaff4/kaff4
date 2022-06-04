package com.github.nava2.aff4.streams.compression

import com.github.nava2.aff4.model.rdf.CompressionMethod
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import org.xerial.snappy.Snappy
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnappyCompression @Inject constructor(
  valueFactory: ValueFactory,
) : CompressionMethod {
  override val method: IRI = valueFactory.createIRI("http://code.google.com/p/snappy/")

  override fun compress(uncompressed: ByteBuffer, compressed: ByteBuffer): Int {
    return Snappy.compress(uncompressed, compressed)
  }

  override fun uncompress(compressed: ByteBuffer, uncompressed: ByteBuffer): Int {
    if (!Snappy.isValidCompressedBuffer(compressed)) return 0

    return Snappy.uncompress(compressed, uncompressed)
  }
}
