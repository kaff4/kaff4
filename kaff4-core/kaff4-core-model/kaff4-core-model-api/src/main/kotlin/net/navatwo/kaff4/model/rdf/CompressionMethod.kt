package net.navatwo.kaff4.model.rdf

import java.nio.ByteBuffer

interface CompressionMethod {
  val method: String?

  /**
   * Checks if [compressed] stores compressed data.
   *
   * @return true if compressed.
   *
   * **Note:** [compressed] is always reset to it's original state.
   */
  fun isCompressed(compressed: ByteBuffer): Boolean

  /**
   * Attempts to compress [source] into [destination]
   *
   * If unable to compress, returning [NOT_COMPRESSED_SENTINEL_VALUE] will cause the data to always be ignored.
   *
   * **Note:** [source] and [destination] are always reset to their original states.
   *
   * @return The byte size of the now compressed data in [destination].
   */
  fun compress(
    source: ByteBuffer,
    destination: ByteBuffer,
  ): Int

  /**
   * Attempts to uncompress [source] into [destination].
   *
   * If unable to uncompress, will return [NOT_UNCOMPRESSED_SENTINEL_VALUE].
   *
   * **Note:** [source] and [destination] are always reset to their original states.
   *
   * @return The byte size of the uncompressed data.
   */
  fun uncompress(
    source: ByteBuffer,
    destination: ByteBuffer,
  ): Int

  companion object {
    /**
     * Value returned by implementations when unable to [compress] data.
     */
    const val NOT_COMPRESSED_SENTINEL_VALUE: Int = Int.MAX_VALUE

    /**
     * Value returned by implementations when unable to [uncompress] data.
     */
    const val NOT_UNCOMPRESSED_SENTINEL_VALUE: Int = 0
  }
}
