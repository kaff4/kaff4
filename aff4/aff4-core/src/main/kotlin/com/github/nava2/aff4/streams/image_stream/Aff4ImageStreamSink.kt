package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.io.TeeSink
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.streams.Aff4Sink
import com.github.nava2.aff4.streams.hashingSink
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.Buffer
import okio.FileSystem
import okio.HashingSink
import okio.Sink
import okio.Timeout
import okio.blackholeSink
import okio.buffer

internal class Aff4ImageStreamSink @AssistedInject constructor(
  private val bevyFactory: Bevy.Factory,
  @Assisted private val outputFileSystem: FileSystem,
  @Assisted imageStream: ImageStream,
  @Assisted private val blockHashTypes: Collection<HashType>,
  @Assisted private val timeout: Timeout,
) : Aff4Sink {
  override val arn: Aff4Arn = imageStream.arn

  var imageStream: ImageStream = imageStream
    private set

  override val model: ImageStream by ::imageStream

  private val bevyMaxSize = imageStream.bevyMaxSize

  private var dataPosition = 0L

  // write-forward only, size is just position
  override val size: Long get() = dataPosition

  private var currentBevySink: Aff4BevySink

  init {
    currentBevySink = Aff4BevySink(
      outputFileSystem = outputFileSystem,
      timeout = timeout,
      imageStream = imageStream,
      bevy = bevyFactory.create(imageStream, 0, blockHashTypes),
    )
  }

  private val linearHashSinks: Map<HashType, HashingSink>
  private val linearHashSink: Sink

  init {
    var baseSink = blackholeSink()
    linearHashSinks = imageStream.linearHashes.associate { hash ->
      val hashType = hash.hashType

      val hashingSink = hashType.hashingSink(baseSink)
      baseSink = hashingSink

      hashType to hashingSink
    }

    linearHashSink = baseSink.buffer()
  }

  private var writingSink: Sink = TeeSink(listOf(linearHashSink, currentBevySink), timeout)

  @Volatile
  private var closed = false

  override fun write(source: Buffer, byteCount: Long) {
    check(!closed) { "closed" }

    var bytesRemaining = byteCount
    while (bytesRemaining > 0L) {
      timeout.throwIfReached()
      maybeUpdateBevySource()

      val bytesRemainingInBevy = bevyMaxSize - (dataPosition % bevyMaxSize)
      val bytesToWrite = bytesRemaining.coerceAtMost(bytesRemainingInBevy)
      writingSink.write(source, bytesToWrite)

      bytesRemaining -= bytesToWrite
      dataPosition += bytesToWrite
    }
  }

  override fun flush() {
    check(!closed) { "closed" }

    writingSink.flush()
  }

  override fun timeout(): Timeout = timeout

  override fun close() {
    if (closed) return

    synchronized(this) {
      if (closed) return
      closed = true
    }

    writingSink.close()
    currentBevySink.close()

    linearHashSink.close()

    for (linearHashSink in linearHashSinks.values) {
      linearHashSink.close()
    }

    imageStream = imageStream.copy(
      size = dataPosition,
      linearHashes = linearHashSinks.entries.asSequence()
        .map { (hashType, sink) -> hashType.value(sink.hash) }
        .toSet(),
    )
  }

  private fun maybeUpdateBevySource() {
    val nextBevyIndex = dataPosition.floorDiv(bevyMaxSize).toInt()
    if (nextBevyIndex == currentBevySink.bevy.index) {
      return
    }

    writingSink.close()
    currentBevySink.close()

    currentBevySink = Aff4BevySink(
      outputFileSystem = outputFileSystem,
      timeout = timeout,
      imageStream = imageStream,
      bevy = bevyFactory.create(imageStream, nextBevyIndex, blockHashTypes),
    )

    writingSink = TeeSink(listOf(linearHashSink, currentBevySink), timeout)
  }
}
