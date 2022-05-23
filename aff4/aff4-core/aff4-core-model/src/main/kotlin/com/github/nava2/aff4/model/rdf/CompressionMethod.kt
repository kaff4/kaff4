package com.github.nava2.aff4.model.rdf

import org.eclipse.rdf4j.model.IRI
import java.nio.ByteBuffer

interface CompressionMethod {
  val method: IRI?

  fun compress(
    uncompressed: ByteBuffer,
    compressed: ByteBuffer,
  ): Int

  fun uncompress(
    compressed: ByteBuffer,
    uncompressed: ByteBuffer,
  ): Int

  object None : CompressionMethod {
    override val method: IRI? = null

    override fun compress(uncompressed: ByteBuffer, compressed: ByteBuffer): Int {
      return copyAndTruncate(uncompressed, compressed)
    }

    override fun uncompress(compressed: ByteBuffer, uncompressed: ByteBuffer): Int {
      return copyAndTruncate(compressed, uncompressed)
    }

    private fun copyAndTruncate(src: ByteBuffer, dst: ByteBuffer): Int {
      val srcRemaining = src.remaining()

      dst.mark()
      src.mark()

      dst.put(src)

      dst.reset()
      src.reset()
      return srcRemaining
    }
  }
}
