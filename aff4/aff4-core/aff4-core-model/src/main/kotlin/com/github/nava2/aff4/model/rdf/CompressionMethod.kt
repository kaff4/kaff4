package com.github.nava2.aff4.model.rdf

import org.eclipse.rdf4j.model.IRI
import java.nio.ByteBuffer

interface CompressionMethod {
  val method: IRI

  fun compress(
    uncompressed: ByteBuffer,
    compressed: ByteBuffer,
  ): Int

  fun uncompress(
    compressed: ByteBuffer,
    uncompressed: ByteBuffer,
  ): Int
}
