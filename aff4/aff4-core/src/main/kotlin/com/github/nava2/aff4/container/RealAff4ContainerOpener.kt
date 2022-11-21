package com.github.nava2.aff4.container

import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.aff4.meta.rdf.ContainerArn
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.Aff4Container.ToolMetadata
import com.github.nava2.aff4.model.Aff4ContainerOpener
import com.github.nava2.aff4.model.Aff4ContainerOpener.Aff4ContainerWithResources
import com.github.nava2.guice.key
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject
import javax.inject.Named

internal class RealAff4ContainerOpener @Inject constructor(
  @Named("containerScope") private val containerScope: ContainerScope,
  private val valueFactory: ValueFactory,
  private val aff4ContainerProvider: com.google.inject.Provider<Aff4Container>,
) : Aff4ContainerOpener {
  override fun manualOpen(fileSystem: FileSystem, path: Path): Aff4ContainerWithResources {
    val imageFileSystem = openImageFileSystem(fileSystem, path)

    val containerMetadata = ToolMetadata.loadFromImage(imageFileSystem)

    val containerArn = imageFileSystem.read("container.description".toPath()) {
      val arn = readString(Charsets.UTF_8).trimEnd()
      valueFactory.createIRI(arn)
    }

    containerScope.enter()

    containerScope.seed(key(ForImageRoot::class), imageFileSystem)
    containerScope.seed(key<IRI>(ContainerArn::class), containerArn)
    containerScope.seed(key(), containerMetadata)

    val aff4Container = containerScope.scope(key(), aff4ContainerProvider).get()
    return Aff4ContainerWithResources(aff4Container) { containerScope.exit() }
  }

  private fun openImageFileSystem(fileSystem: FileSystem, path: Path): FileSystem {
    return if (path.toFile().extension in setOf("zip", "aff4")) {
      fileSystem.openZip(path)
    } else {
      fileSystem.relativeTo(path)
    }
  }
}

private fun ToolMetadata.Companion.loadFromImage(imageFileSystem: FileSystem): ToolMetadata {
  val (version, tool) = imageFileSystem.read("version.txt".toPath()) {
    var major = -1
    var minor = -1
    lateinit var tool: String
    for (line in readString(Charsets.UTF_8).lineSequence().filter { it.isNotEmpty() }) {
      val (tag, value) = line.split('=')
      when (tag) {
        "major" -> major = value.toInt()
        "minor" -> minor = value.toInt()
        "tool" -> tool = value
      }
    }

    check(major >= 0 && minor >= 0) { "Invalid version.txt" }
    "$major.$minor" to tool
  }

  return ToolMetadata(version, tool)
}
