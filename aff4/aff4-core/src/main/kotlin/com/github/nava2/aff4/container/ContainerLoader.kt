package com.github.nava2.aff4.container

import com.github.nava2.aff4.container.RealAff4ImageOpener.LoadedContainersContext
import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.rdf.Aff4Arn
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject

internal class ContainerLoader @Inject constructor(
  private val valueFactory: ValueFactory,
) {
  fun getContainersForImage(fileSystem: FileSystem, openPath: Path): List<LoadedContainersContext.Container> {
    return listOf(
      openContainer(fileSystem, openPath),
    )
  }

  private fun openContainer(fileSystem: FileSystem, path: Path): LoadedContainersContext.Container {
    val dataFileSystem = openDataFileSystem(fileSystem, path)
    val containerArn = dataFileSystem.readContainerArn()
    val toolMetadata = Aff4Container.ToolMetadata.loadFromContainer(dataFileSystem)
    return LoadedContainersContext.Container(
      container = Aff4Container(
        containerArn = containerArn,
        dataFileSystem = dataFileSystem,
        metadata = toolMetadata,
      ),
      imageName = path.getContainerName(),
      containerFileSystem = fileSystem,
      containerPath = path,
    )
  }

  private fun openDataFileSystem(fileSystem: FileSystem, path: Path): FileSystem {
    val metadata = fileSystem.metadata(path)
    return if (metadata.isRegularFile) {
      fileSystem.openZip(path)
    } else {
      fileSystem.relativeTo(path)
    }
  }

  private fun FileSystem.readContainerArn(): Aff4Arn {
    return read("container.description".toPath()) {
      val arn = readString(Charsets.UTF_8).trimEnd()
      valueFactory.createIRI(arn)
    }
  }
}

private fun Path.getContainerName(): String {
  return segments.last().substringBeforeLast(".aff4")
}

private fun Aff4Container.ToolMetadata.Companion.loadFromContainer(
  imageFileSystem: FileSystem,
): Aff4Container.ToolMetadata {
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

  return Aff4Container.ToolMetadata(version, tool)
}
