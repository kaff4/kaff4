package net.navatwo.kaff4.streams.map_stream

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import net.navatwo.kaff4.container.ContainerDataFileSystemProvider
import net.navatwo.kaff4.io.Source
import net.navatwo.kaff4.io.concatLazily
import net.navatwo.kaff4.io.sourceProvider
import net.navatwo.kaff4.io.use
import net.navatwo.kaff4.model.Aff4Model
import net.navatwo.kaff4.model.Aff4StreamOpener
import net.navatwo.kaff4.model.VerifiableStreamProvider
import net.navatwo.kaff4.model.VerifiableStreamProvider.Result.FailedHash
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.Hash
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.streams.computeLinearHash
import net.navatwo.kaff4.yieldNotNull
import okio.Path
import okio.Timeout

internal class RealAff4MapStreamSourceProvider @AssistedInject constructor(
  private val aff4StreamOpener: Aff4StreamOpener,
  private val mapStreamMapReader: MapStreamMapReader,
  containerDataFileSystemProvider: ContainerDataFileSystemProvider,
  @Assisted override val mapStream: MapStream,
) : Aff4MapStreamSourceProvider {

  private val imageFileSystem by containerDataFileSystemProvider.lazy(mapStream)
  private val map: MapStreamMap by lazy { mapStreamMapReader.loadMap(mapStream) }

  override val arn: Aff4Arn = mapStream.arn
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

      failedHashes += mapStream.computeMapComponentHashes(timeout)

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
  private fun MapStream.computeMapComponentHashes(timeout: Timeout): Sequence<FailedHash> = sequence {
    yieldNotNull(mapIdxHash?.maybeValidateHashComponent("idx", idxPath, timeout))
    yieldNotNull(mapPointHash?.maybeValidateHashComponent("mapPoint", mapPath, timeout))
    yieldNotNull(mapPathHash?.maybeValidateHashComponent("mapPath", mapPathPath, timeout))

    val mapHash = mapHash
    if (mapHash != null) {
      val maybeFailure = listOfNotNull(
        // H( map || idx || [mapPath] )
        mapPath,
        idxPath,
        mapPathPath.takeIf { imageFileSystem.exists(it) },
      )
        .map { imageFileSystem.sourceProvider(it) }
        .concatLazily()
        .use { source ->
          val actualHash = source.computeLinearHash(mapHash.hashType)
          if (actualHash != mapHash.value) {
            FailedHash(this@computeMapComponentHashes, "map", mapHash, actualHash)
          } else {
            null
          }
        }

      yieldNotNull(maybeFailure)
    }
  }

  private fun Hash.maybeValidateHashComponent(
    key: String,
    path: Path,
    timeout: Timeout,
  ): FailedHash? {
    val actualHash = imageFileSystem.sourceProvider(path).use(timeout) { source ->
      source.computeLinearHash(hashType)
    }

    return FailedHash(mapStream, key, this, actualHash).takeIf { actualHash != value }
  }
}
