package com.github.nava2.aff4.container

import com.github.nava2.aff4.Aff4TestModule
import com.github.nava2.aff4.ForImages
import com.github.nava2.aff4.container.RealAff4ImageOpener.LoadedContainersContext
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.rdf.createArn
import com.github.nava2.aff4.satisfies
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.test.GuiceModule
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.openZip
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.junit.jupiter.api.Test
import javax.inject.Inject

internal class ContainerLoaderTest {
  @GuiceModule
  val testModules = listOf(
    Aff4TestModule,
    object : KAbstractModule() {
      override fun configure() {
        bind<ValueFactory>().toInstance(SimpleValueFactory.getInstance())
      }
    }
  )

  @Inject
  private lateinit var containerLoader: ContainerLoader

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  @field:ForImages
  private lateinit var imagesFileSystem: FileSystem

  @Test
  fun `getContainersForImage opens an Base-Linear and extracts metadata`() {
    val imagePath = "Base-Linear.aff4".toPath()
    val containers = containerLoader.getContainersForImage(imagesFileSystem, imagePath)
    assertThat(containers).singleElement().isEqual(
      LoadedContainersContext.Container(
        imageName = "Base-Linear",
        containerFileSystem = imagesFileSystem,
        containerPath = imagePath,
        container = Aff4Container(
          containerArn = arn("aff4://685e15cc-d0fb-4dbc-ba47-48117fc77044"),
          metadata = Aff4Container.ToolMetadata("1.0", "Evimetry 2.2.0"),
          dataFileSystem = imagesFileSystem.openZip(imagePath),
        ),
      )
    )
  }

  @Test
  fun `getContainersForImage opens dream and extracts metadata`() {
    val imagePath = "dream.aff4".toPath()
    val containers = containerLoader.getContainersForImage(imagesFileSystem, imagePath)
    assertThat(containers).singleElement().isEqual(
      LoadedContainersContext.Container(
        imageName = "dream",
        containerFileSystem = imagesFileSystem,
        containerPath = imagePath,
        container = Aff4Container(
          containerArn = arn("aff4://5aea2dd0-32b4-4c61-a9db-677654be6f83"),
          metadata = Aff4Container.ToolMetadata("1.1", "pyaff4"),
          dataFileSystem = imagesFileSystem.openZip(imagePath),
        ),
      )
    )
  }

  @Test
  fun `getContainersForImage opens nested images and extracts metadata`() {
    val imagePath = "nested_logical_images.aff4".toPath()
    val containers = containerLoader.getContainersForImage(imagesFileSystem, imagePath)
    assertThat(containers).singleElement().isEqual(
      LoadedContainersContext.Container(
        imageName = "nested_logical_images",
        containerFileSystem = imagesFileSystem,
        containerPath = imagePath,
        container = Aff4Container(
          containerArn = arn("aff4://2872342b-8aff-4747-9325-6cd5f50bcff5"),
          metadata = Aff4Container.ToolMetadata("1.1", "pyaff4"),
          dataFileSystem = imagesFileSystem.openZip(imagePath),
        ),
      )
    )
  }

  private fun arn(iri: String) = valueFactory.createArn(iri)
}

private fun <SELF : AbstractObjectAssert<SELF, LoadedContainersContext.Container>> SELF.isEqual(
  expected: LoadedContainersContext.Container,
): SELF = satisfies { container ->
  SoftAssertions.assertSoftly { assert ->
    assert.assertThat(container.imageName)
      .`as` { "imageName" }
      .isEqualTo(expected.imageName)

    assert.assertThat(container.containerFileSystem)
      .`as` { "containerFileSystem" }
      .isEqualTo(expected.containerFileSystem)

    assert.assertThat(container.containerPath)
      .`as` { "containerPath" }
      .isEqualTo(expected.containerPath)

    assert.assertThat(container.container.containerArn)
      .`as` { "container.containerArn" }
      .isEqualTo(expected.container.containerArn)
    assert.assertThat(container.container.metadata)
      .`as` { "container.metadata" }
      .isEqualTo(expected.container.metadata)

    // Equality doesn't make sense, so check type
    assert.assertThat(container.container.dataFileSystem)
      .`as` { "container.dataFileSystem" }
      .isInstanceOf(expected.container.dataFileSystem.javaClass)
  }
}
