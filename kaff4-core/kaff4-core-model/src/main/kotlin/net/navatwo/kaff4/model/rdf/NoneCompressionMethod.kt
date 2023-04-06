package net.navatwo.kaff4.model.rdf

import net.navatwo.kaff4.model.rdf.CompressionMethod.Companion.NOT_COMPRESSED_SENTINEL_VALUE
import net.navatwo.kaff4.model.rdf.CompressionMethod.Companion.NOT_UNCOMPRESSED_SENTINEL_VALUE
import java.nio.ByteBuffer

internal object NoneCompressionMethod : CompressionMethod {

  override val method: String? = null

  override fun isCompressed(compressed: ByteBuffer): Boolean = false

  override fun compress(source: ByteBuffer, destination: ByteBuffer): Int = NOT_COMPRESSED_SENTINEL_VALUE

  override fun uncompress(source: ByteBuffer, destination: ByteBuffer): Int = NOT_UNCOMPRESSED_SENTINEL_VALUE

  override fun toString(): String = javaClass.simpleName
}

val CompressionMethod.Companion.None: CompressionMethod
  get() = NoneCompressionMethod
