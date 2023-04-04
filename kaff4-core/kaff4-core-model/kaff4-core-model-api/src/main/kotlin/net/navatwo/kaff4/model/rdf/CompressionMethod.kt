package net.navatwo.kaff4.model.rdf

import java.nio.ByteBuffer

interface CompressionMethod {
  val method: String?

  fun compress(
    uncompressed: ByteBuffer,
    compressed: ByteBuffer,
  ): Int

  fun uncompress(
    compressed: ByteBuffer,
    uncompressed: ByteBuffer,
  ): Int

  companion object
}
