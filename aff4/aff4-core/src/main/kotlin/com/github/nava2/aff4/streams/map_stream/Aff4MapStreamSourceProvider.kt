package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.io.concatLazily
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
import com.github.nava2.aff4.streams.computeLinearHash
import com.github.nava2.aff4.yieldNotNull
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.FileSystem
import okio.Path
import okio.Source
import okio.Timeout
import org.eclipse.rdf4j.model.IRI

class Aff4MapStreamSourceProvider @AssistedInject internal constructor(
  private val aff4StreamOpener: Aff4StreamOpener,
  private val mapStreamMapReader: MapStreamMapReader,
  @ForImageRoot private val imageFileSystem: FileSystem,
  @Assisted val mapStream: MapStream,
) : Aff4StreamSourceProvider, VerifiableStreamProvider {
  private val map: MapStreamMap by lazy { mapStreamMapReader.loadMap(mapStream) }

  override val size: Long = mapStream.size

  @Volatile
  private var verificationResult: VerifiableStreamProvider.Result? = null

  override fun source(position: Long, timeout: Timeout): Source {
    return Aff4MapStreamSource(
      aff4StreamOpener = aff4StreamOpener,
      mapStream = mapStream,
      map = map,
      position = position,
      timeout = timeout,
    )
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

  interface Loader : Aff4StreamSourceProvider.Loader<MapStream, Aff4MapStreamSourceProvider>
}
