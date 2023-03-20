package com.github.nava2.aff4.container

import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.Aff4Image
import com.github.nava2.aff4.model.Aff4ImageOpener
import com.github.nava2.aff4.model.Aff4ImageOpener.Aff4ImageWithResources
import com.github.nava2.guice.action_scoped.ActionScope
import com.github.nava2.guice.key
import okio.FileSystem
import okio.Path
import javax.inject.Inject

internal class RealAff4ImageOpener @Inject constructor(
  private val actionScope: ActionScope,
  private val containerLoader: ContainerLoader,
  private val aff4ImageProvider: com.google.inject.Provider<Aff4Image>,
) : Aff4ImageOpener {
  override fun manualOpen(fileSystem: FileSystem, path: Path): Aff4ImageWithResources {
    val containers = containerLoader.getContainersForImage(fileSystem, path)
    check(containers.isNotEmpty()) {
      "Failed to load image [$path]: no containers found."
    }

    val openedContainer = containers.single { it.containerPath == path }

    val action = actionScope.start(
      mapOf(
        key<LoadedContainersContext>() to LoadedContainersContext(imageName = openedContainer.imageName, containers),
      )
    )

    val aff4Image = actionScope.scope(key<Aff4Image>(), aff4ImageProvider).get()
    return Aff4ImageWithResources(aff4Image) { action.close() }
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
