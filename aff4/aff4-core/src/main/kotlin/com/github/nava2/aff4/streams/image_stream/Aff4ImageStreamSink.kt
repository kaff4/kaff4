package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.io.TeeSink
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.streams.hashingSink
import okio.Buffer
import okio.FileSystem
import okio.HashingSink
import okio.Sink
import okio.Timeout
import okio.blackholeSink
import okio.buffer
import org.eclipse.rdf4j.model.IRI
import javax.inject.Named

class Aff4ImageStreamSink(
  private val bevyFactory: Bevy.Factory,
  @Named("ImageOutput") private val outputFileSystem: FileSystem,
  imageStream: ImageStream,
  private val blockHashTypes: Collection<HashType>,
  private val timeout: Timeout,
) : Sink {
  val arn: IRI = imageStream.arn

  var imageStream: ImageStream = imageStream
    private set

  private val bevyMaxSize = imageStream.bevyMaxSize

  var dataPosition = 0L
    private set

  // write-forward only, size is just position
  val size: Long get() = dataPosition

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
      linearHashes = linearHashSinks.entries.map { (hashType, sink) -> hashType.value(sink.hash) },
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
