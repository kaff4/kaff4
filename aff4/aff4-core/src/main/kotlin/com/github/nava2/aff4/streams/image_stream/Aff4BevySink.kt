package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.io.source
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.streams.computeLinearHashes
import okio.Buffer
import okio.FileSystem
import okio.Sink
import okio.Timeout
import okio.buffer
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Named

internal class Aff4BevySink @Inject constructor(
  @Named("ImageOutput") private val outputFileSystem: FileSystem,
  private val timeout: Timeout,
  imageStream: ImageStream,
  val bevy: Bevy,
) : Sink {
  private val bevyMaxSize = imageStream.bevyMaxSize
  private val compressionMethod = imageStream.compressionMethod

  private var dataPosition = 0L
  private val dataSink = outputFileSystem.sink(bevy.dataSegment).buffer()

  private val indexSink = outputFileSystem.sink(bevy.indexSegment).buffer()
  private val hashSinks = bevy.blockHashes.mapValues { (_, path) -> outputFileSystem.sink(path).buffer() }

  private val uncompressedChunkBuffer = ByteBuffer.allocateDirect(imageStream.chunkSize)
  private val compressedChunkBuffer = ByteBuffer.allocateDirect(imageStream.chunkSize)
  private var bytesWritten = 0L

  @Volatile
  private var closed = false

  override fun write(source: Buffer, byteCount: Long) {
    check(!closed) { "closed" }

    require(bytesWritten + byteCount <= bevyMaxSize) {
      "Can not overflow bevy bytes: $bytesWritten + $byteCount <= $bevyMaxSize"
    }

    var bytesRemaining = byteCount
    while (bytesRemaining > 0L) {
      timeout.throwIfReached()

      if (uncompressedChunkBuffer.hasRemaining()) {
        val bytesFromSource = source.read(uncompressedChunkBuffer)
        if (bytesFromSource == -1) break

        bytesRemaining -= bytesFromSource
      }

      if (!uncompressedChunkBuffer.hasRemaining()) {
        // time to flush!
        writeBuffersToSinks()
      }
    }

    bytesWritten += byteCount - bytesRemaining
  }

  override fun flush() {
    check(!closed) { "closed" }

    writeBuffersToSinks()

    for (sink in sinks()) {
      sink.flush()
    }
  }

  override fun timeout(): Timeout = timeout

  override fun close() {
    if (closed) return

    synchronized(this) {
      if (closed) return
      closed = true
    }

    writeBuffersToSinks()

    for (sink in sinks()) {
      sink.close()
    }
  }

  private fun writeBuffersToSinks() {
    if (uncompressedChunkBuffer.remaining() == uncompressedChunkBuffer.limit()) {
      // Nothing to write, don't bother trying to flush
      return
    }

    timeout.throwIfReached()

    uncompressedChunkBuffer.limit(uncompressedChunkBuffer.position())
    uncompressedChunkBuffer.rewind()

    compressedChunkBuffer.rewind()
    compressedChunkBuffer.limit(compressedChunkBuffer.capacity())

    val compressedSize = compressionMethod.compress(uncompressedChunkBuffer, compressedChunkBuffer)

    val bufferToWrite = if (compressedSize >= uncompressedChunkBuffer.limit()) {
      uncompressedChunkBuffer
    } else {
      compressedChunkBuffer
    }

    val dataLength = bufferToWrite.limit()

    indexSink.writeLongLe(dataPosition)
    indexSink.writeIntLe(dataLength)

    dataSink.write(bufferToWrite)
    dataPosition += dataLength

    bufferToWrite.rewind()
    val hashes = bufferToWrite.source(timeout).buffer().use { source ->
      source.computeLinearHashes(bevy.blockHashes.keys)
    }

    for ((hashType, hash) in hashes) {
      val sink = hashSinks.getValue(hashType)
      sink.write(hash)
    }

    uncompressedChunkBuffer.rewind()
    uncompressedChunkBuffer.limit(uncompressedChunkBuffer.capacity())
  }

  private fun sinks(): Sequence<Sink> = sequence {
    yield(dataSink)
    yield(indexSink)
    yieldAll(hashSinks.values)
  }
}
