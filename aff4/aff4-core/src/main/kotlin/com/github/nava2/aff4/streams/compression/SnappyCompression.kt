package com.github.nava2.aff4.streams.compression

import com.github.nava2.aff4.meta.rdf.model.CompressionMethod
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import org.xerial.snappy.Snappy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SnappyCompression @Inject constructor(
  valueFactory: ValueFactory,
) : CompressionMethod {
  override val method: IRI = valueFactory.createIRI("http://code.google.com/p/snappy/")

  override fun compress(
    buffer: ByteArray,
    bufferStart: Int,
    length: Int,
    output: ByteArray,
    outputStart: Int,
  ): Int {
    return Snappy.compress(
      buffer,
      bufferStart,
      length,
      output,
      outputStart,
    )
  }

  override fun uncompress(
    buffer: ByteArray,
    bufferStart: Int,
    length: Int,
    output: ByteArray,
    outputStart: Int,
  ): Int {
    return Snappy.uncompress(
      buffer,
      bufferStart,
      length,
      output,
      outputStart,
    )
  }
}
