package com.github.nava2.aff4.model.rdf

import java.nio.ByteBuffer

interface CompressionMethod {
  val method: org.eclipse.rdf4j.model.IRI?

  fun compress(
    uncompressed: ByteBuffer,
    compressed: ByteBuffer,
  ): Int

  fun uncompress(
    compressed: ByteBuffer,
    uncompressed: ByteBuffer,
  ): Int

  object None : CompressionMethod {
    override val method: org.eclipse.rdf4j.model.IRI? = null

    override fun compress(uncompressed: ByteBuffer, compressed: ByteBuffer): Int {
      return copyAndTruncate(uncompressed, compressed)
    }

    override fun uncompress(compressed: ByteBuffer, uncompressed: ByteBuffer): Int {
      return copyAndTruncate(compressed, uncompressed)
    }

    override fun toString(): String = javaClass.simpleName

    private fun copyAndTruncate(src: ByteBuffer, dst: ByteBuffer): Int {
      check(dst.remaining() >= src.remaining()) {
        "dst buffer does not have enough remaining [${dst.remaining()}] require [${src.remaining()}]"
      }

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
