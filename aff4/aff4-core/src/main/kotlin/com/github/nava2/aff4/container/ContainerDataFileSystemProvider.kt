package com.github.nava2.aff4.container

import com.github.nava2.aff4.model.Aff4ContainerContext
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.StoredRdfModel
import okio.FileSystem
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
internal class ContainerDataFileSystemProvider @Inject constructor(
  @ContainerScoped private val containerContextProvider: Provider<Aff4ContainerContext>,
) {
  operator fun get(containerArn: Aff4Arn): FileSystem {
    return containerContextProvider.get().imageFileSystem
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
