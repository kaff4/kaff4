package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.io.applyAndCloseOnThrow
import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.concatLazily
import com.github.nava2.aff4.io.limit
import com.github.nava2.aff4.io.sourceProvider
import com.github.nava2.aff4.io.use
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4StreamOpener
import com.github.nava2.aff4.model.Aff4StreamSourceProvider
import com.github.nava2.aff4.model.VerifiableStreamProvider
import com.github.nava2.aff4.model.VerifiableStreamProvider.Result.FailedHash
import com.github.nava2.aff4.model.rdf.Hash
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.streams.SourceProviderWithRefCounts
import com.github.nava2.aff4.streams.computeLinearHash
import com.github.nava2.aff4.yieldNotNull
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.Source
import okio.Timeout
import org.eclipse.rdf4j.model.IRI
import java.io.Closeable

class Aff4MapStreamSourceProvider @AssistedInject internal constructor(
  private val aff4StreamOpener: Aff4StreamOpener,
  private val mapStreamMapReader: MapStreamMapReader,
  @ForImageRoot private val imageFileSystem: FileSystem,
  @Assisted val mapStream: MapStream,
) : Aff4StreamSourceProvider, VerifiableStreamProvider, SourceProviderWithRefCounts.SourceDelegate {
  private val sourceProviderWithRefCounts = SourceProviderWithRefCounts(this)

  private val map by lazy { mapStreamMapReader.loadMap(mapStream) }

  override val size: Long = mapStream.size

  private var position: Long = 0L

  private var currentSource: CurrentSourceInfo? = null

  @Volatile
  private var verificationResult: VerifiableStreamProvider.Result? = null

  override fun source(position: Long, timeout: Timeout): Source = sourceProviderWithRefCounts.source(position, timeout)

  override fun close() {
    resetCurrentSource()
    sourceProviderWithRefCounts.close()
  }

  override fun verify(aff4Model: Aff4Model, timeout: Timeout): VerifiableStreamProvider.Result {
    val previousResult = verificationResult
    if (previousResult != null) {
      return previousResult
    }

    return synchronized(this) {
      val doubleChecked = verificationResult
      if (doubleChecked != null) return doubleChecked

      val failedHashes = mutableListOf<FailedHash>()

      failedHashes += computeMapComponentHashes(aff4Model.containerArn, timeout)

      val imageStreamResult = mapStream.dependentStream
        ?.let { aff4StreamOpener.openStream(it) as VerifiableStreamProvider }
        ?.verify(aff4Model, timeout)
        ?: VerifiableStreamProvider.Result.Success
      failedHashes += imageStreamResult.failedHashes

      // TODO Map block hash

      val result = VerifiableStreamProvider.Result.fromFailedHashes(failedHashes)
      verificationResult = result
      result
    }
  }

  override fun toString(): String {
    return "Aff4MapStream(${mapStream.arn})"
  }

  // https://github.com/aff4/Standard/blob/master/inprogress/AFF4StandardSpecification-v1.0a.md#map-hashes
  private fun computeMapComponentHashes(containerArn: IRI, timeout: Timeout): Sequence<FailedHash> = sequence {
    yieldNotNull(
      maybeValidateHashComponent("idx", mapStream.idxPath(containerArn), mapStream.mapIdxHash, timeout),
    )
    yieldNotNull(
      maybeValidateHashComponent("mapPoint", mapStream.mapPath(containerArn), mapStream.mapPointHash, timeout),
    )
    yieldNotNull(
      maybeValidateHashComponent("mapPath", mapStream.mapPathPath(containerArn), mapStream.mapPathHash, timeout),
    )

    val mapHash = mapStream.mapHash
    if (mapHash != null) {
      val maybeFailure = concatLazily(
        // H( map || idx || [mapPath] )
        sources = listOfNotNull(
          mapStream.mapPath(containerArn),
          mapStream.idxPath(containerArn),
          mapStream.mapPathPath(containerArn).takeIf { imageFileSystem.exists(it) },
        ).map { imageFileSystem.sourceProvider(it) },
      ).use { source ->
        val actualHash = source.computeLinearHash(mapHash.hashType)
        FailedHash(mapStream, "map", mapHash).takeIf { actualHash != mapHash.value }
      }

      yieldNotNull(maybeFailure)
    }
  }

  private fun maybeValidateHashComponent(
    key: String,
    path: Path,
    hash: Hash?,
    timeout: Timeout,
  ): FailedHash? {
    if (hash == null) return null

    val actualHash = imageFileSystem.sourceProvider(path).use(timeout) { source ->
      source.computeLinearHash(hash.hashType)
    }

    return FailedHash(mapStream, key, hash).takeIf { actualHash != hash.value }
  }

  override fun readAt(readPosition: Long, timeout: Timeout, sink: Buffer, byteCount: Long): Long {
    moveTo(readPosition)

    // we are exhausted
    if (position == size) return -1L

    val entryToRead = map.query(position, byteCount).firstOrNull() ?: return -1

    val maxBytesToRead = byteCount.coerceAtMost(entryToRead.length - (position - entryToRead.mappedOffset))

    val readSource = getAndUpdateCurrentSourceIfChanged(position, timeout, entryToRead)

    val bytesRead = readSource.read(sink, maxBytesToRead)
    check(bytesRead >= 0) {
      // because of how we read these targets by capping their read size to the entry.length, we *should* never read
      // them when they are exhausted.
      "Read too much of target [${entryToRead.targetIRI}] - $entryToRead - $mapStream"
    }

    position += bytesRead

    return bytesRead
  }

  private fun getAndUpdateCurrentSourceIfChanged(
    nextPosition: Long,
    timeout: Timeout,
    entryToRead: MapStreamEntry,
  ): BufferedSource {
    val currentSource = currentSource

    if (currentSource?.entry == entryToRead) {
      return currentSource.source
    }

    resetCurrentSource()

    val targetSourceProvider = aff4StreamOpener.openStream(entryToRead.targetIRI)
    val targetSource = targetSourceProvider
      .limit(entryToRead.length)
      .buffer()
      .source(entryToRead.targetOffset, timeout)
      .applyAndCloseOnThrow {
        if (nextPosition != entryToRead.mappedOffset) {
          skip(entryToRead.mappedOffset - nextPosition)
        }
      }

    this.currentSource = CurrentSourceInfo(entryToRead, targetSource)

    return targetSource
  }

  private fun moveTo(newPosition: Long) {
    val cappedPosition = newPosition.coerceAtMost(size)
    if (cappedPosition == position) return

    val currentSource = this.currentSource

    when {
      currentSource == null -> Unit
      cappedPosition > position && currentSource.entry.let { cappedPosition in it } -> {
        // try and skip forwards if its a small gap
        currentSource.source.skip(cappedPosition - position)
      }
      else -> resetCurrentSource()
    }

    position = cappedPosition
  }

  private fun resetCurrentSource() {
    currentSource?.close()
    currentSource = null
  }

  private data class CurrentSourceInfo(
    val entry: MapStreamEntry,
    val source: BufferedSource,
  ) : Closeable by source

  interface Loader : Aff4StreamSourceProvider.Loader<MapStream, Aff4MapStreamSourceProvider>
}
