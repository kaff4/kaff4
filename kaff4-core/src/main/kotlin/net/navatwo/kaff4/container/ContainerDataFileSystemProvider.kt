package net.navatwo.kaff4.container

import misk.scope.ActionScoped
import net.navatwo.kaff4.model.Aff4ImageContext
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.StoredRdfModel
import okio.FileSystem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ContainerDataFileSystemProvider @Inject constructor(
  private val actionScopedContainerContext: ActionScoped<Aff4ImageContext>,
) {
  operator fun get(containerArn: Aff4Arn): FileSystem {
    val containers = actionScopedContainerContext.get().containers

    val requestedContainer = containers.firstOrNull { it.containerArn == containerArn }
      ?: error("Unknown container requested: $containerArn")

    return requestedContainer.dataFileSystem
  }

  fun get(storedRdfModel: StoredRdfModel): FileSystem {
    return get(storedRdfModel.stored)
  }

  fun lazy(volumeArn: Aff4Arn): Lazy<FileSystem> = lazy { get(volumeArn) }

  fun lazy(storedRdfModel: StoredRdfModel): Lazy<FileSystem> {
    val containerArn = storedRdfModel.stored
    return lazy { get(containerArn) }
  }
}
