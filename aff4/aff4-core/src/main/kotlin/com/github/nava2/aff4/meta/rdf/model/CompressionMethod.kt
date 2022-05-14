package com.github.nava2.aff4.meta.rdf.model

import org.eclipse.rdf4j.model.IRI

interface CompressionMethod {
  val method: IRI

  fun compress(
    buffer: ByteArray,
    bufferStart: Int,
    length: Int,
    output: ByteArray,
    outputStart: Int,
  ): Int

  fun uncompress(
    buffer: ByteArray,
    bufferStart: Int,
    length: Int,
    output: ByteArray,
    outputStart: Int,
  ): Int
}
