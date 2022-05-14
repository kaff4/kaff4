package com.github.nava2.aff4.streams

import com.github.nava2.aff4.meta.rdf.model.ImageStream
import okio.FileHandle
import okio.FileSystem
import java.io.InputStream

internal class Aff4BevyInputStream(
  fileSystem: FileSystem,
  imageStreamConfig: ImageStream,
  config: Bevy,
) : InputStream() {
  private val compressionMethod = imageStreamConfig.compressionMethod
  private val chunkSize = imageStreamConfig.chunkSize

  private var position: Long = 0

  private var chunkBufferLength = 0
  private val chunkBuffer = ByteArray(chunkSize)
  private val compressedChunkBuffer = ByteArray(chunkSize)

  private val indexReader: BevyIndexReader = BevyIndexReader(fileSystem, imageStreamConfig, config)

  private val dataStream: FileHandle = fileSystem.openReadOnly(config.dataSegment)

  fun seek(position: Long) {
    this.position = position
    this.chunkBufferLength = 0 // resets the buffer to always be read
  }

  override fun read(): Int {
    val bufferPosition = (position % chunkSize).toInt()
    if (bufferPosition >= chunkBufferLength) {
      if (!readIntoBuffer()) return -1
    }

    val result = chunkBuffer[bufferPosition]
    position += 1
    return result.toInt()
  }

  private fun readIntoBuffer(): Boolean {
    val index = indexReader.readIndexContaining(position) ?: return false
    check(index.compressedLength <= chunkSize) {
      "Read invalid compressed chunk index.length: ${index.compressedLength}"
    }

    val dataReadResult = dataStream.read(index.bevyPosition, compressedChunkBuffer, 0, index.compressedLength)
    check(dataReadResult == index.compressedLength)

    val chunkBufferLength =
      compressionMethod.uncompress(compressedChunkBuffer, 0, index.compressedLength, chunkBuffer, 0)
    if (chunkBufferLength > index.compressedLength) {
      this.chunkBufferLength = chunkBufferLength
    } else {
      // data wasn't compressed, so 1-1 copy it
      compressedChunkBuffer.copyInto(chunkBuffer, 0, index.compressedLength)
    }

    return true
  }

  override fun close() {
    super.close()

    indexReader.close()
    dataStream.close()
  }
}
