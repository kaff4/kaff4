package net.navatwo.kaff4.io

import okio.Buffer
import okio.Timeout
import java.io.Closeable

interface Source : AutoCloseable, Closeable {

  /**
   * Removes at least 1, and up to `byteCount` bytes from this and appends them to `sink`. Returns
   * the number of bytes read, or -1 if this source is exhausted.
   */
  fun read(sink: Buffer, byteCount: Long): Long

  /** Returns the timeout for this source.  */
  fun timeout(): Timeout

  /**
   * Closes this source and releases the resources held by this source. It is an error to read a
   * closed source. It is safe to close a source more than once.
   */
  override fun close()

  fun asOkio(): okio.Source = OkioSource(this)

  private class OkioSource(private val source: Source) : okio.Source {
    override fun close() = source.close()

    override fun read(sink: Buffer, byteCount: Long): Long = source.read(sink, byteCount)

    override fun timeout(): Timeout = source.timeout()
  }
}
