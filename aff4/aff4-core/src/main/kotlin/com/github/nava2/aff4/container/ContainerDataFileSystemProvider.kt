package com.github.nava2.aff4.container

import com.github.nava2.aff4.model.Aff4ImageContext
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.StoredRdfModel
import misk.scope.ActionScoped
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
