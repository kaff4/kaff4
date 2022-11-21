package com.github.nava2.aff4.container

import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.Aff4Image
import com.github.nava2.aff4.model.Aff4ImageOpener
import com.github.nava2.aff4.model.Aff4ImageOpener.Aff4ContainerWithResources
import com.github.nava2.guice.key
import okio.FileSystem
import okio.Path
import javax.inject.Inject
import javax.inject.Named

internal class RealAff4ImageOpener @Inject constructor(
  @Named("imageScope") private val imageScope: ImageScope,
  private val containerLoader: ContainerLoader,
  private val aff4ImageProvider: com.google.inject.Provider<Aff4Image>,
) : Aff4ImageOpener {
  override fun manualOpen(fileSystem: FileSystem, path: Path): Aff4ContainerWithResources {
    val containers = containerLoader.getContainersForImage(fileSystem, path)
    check(containers.isNotEmpty()) {
      "Failed to load image [$path]: no containers found."
    }

    val openedContainer = containers.single { it.containerPath == path }

    imageScope.enter()

    imageScope.seed(key(), LoadedContainersContext(imageName = openedContainer.imageName, containers))

    val aff4Container = imageScope.scope(key(), aff4ImageProvider).get()
    return Aff4ContainerWithResources(aff4Container) { imageScope.exit() }
  }

  data class LoadedContainersContext(
    val imageName: String,
    val containers: List<Container>,
  ) {
    data class Container(
      val imageName: String,
      val container: Aff4Container,
      val containerFileSystem: FileSystem,
      val containerPath: Path,
    )
  }
}
