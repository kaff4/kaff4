package net.navatwo.kaff4.container

import com.google.inject.util.Modules
import jakarta.inject.Inject
import net.navatwo.kaff4.Aff4ImageTestModule
import net.navatwo.kaff4.Aff4TestModule
import net.navatwo.kaff4.BaseLinear
import net.navatwo.kaff4.Dream
import net.navatwo.kaff4.ForImages
import net.navatwo.kaff4.TestActionScopeModule
import net.navatwo.kaff4.TestToolDialectModule
import net.navatwo.kaff4.UsingTemporary
import net.navatwo.kaff4.container.RealAff4ImageOpener.LoadedContainersContext
import net.navatwo.kaff4.model.Aff4Container
import net.navatwo.kaff4.model.rdf.createArn
import net.navatwo.kaff4.satisfies
import net.navatwo.test.GuiceModule
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

internal class ContainerLoaderTest {
  companion object {
    @JvmStatic
    @Suppress("UnusedPrivateMember")
    private fun maybeExtractStripeIndexValidStripesProvider(): Stream<Arguments> = Stream.of(
      Arguments.of("BaseLinear_1.aff4", "BaseLinear", 1),
      Arguments.of("BaseLinear_2.aff4", "BaseLinear", 2),
      Arguments.of("BaseLinear.good_2.aff4", "BaseLinear.good", 2),
      Arguments.of("BaseLinear.3.aff4", "BaseLinear", 3),
      Arguments.of("BaseLinear.aff4.0", "BaseLinear", 0),
      Arguments.of("BaseLinear.aff4.001", "BaseLinear", 1),
    )
  }

  @GuiceModule
  val module = Modules.combine(
    Aff4TestModule,
    TestActionScopeModule,
    TestToolDialectModule,
  )

  @Inject
  private lateinit var containerLoader: ContainerLoader

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  @field:ForImages
  private lateinit var imagesFileSystem: FileSystem

  @UsingTemporary
  private lateinit var workingFileSystem: FileSystem

  @Test
  fun `getContainersForImage opens an Base-Linear and extracts metadata`() {
    val imagePath = Aff4ImageTestModule.BaseLinear.imagePath
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
    val imagePath = Aff4ImageTestModule.Dream.imagePath
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

  @ParameterizedTest(name = "maybeExtractStripeIndex({0}) = ({1}, {2})")
  @MethodSource("maybeExtractStripeIndexValidStripesProvider")
  fun `maybeExtractStripeIndex extract valid stripes`(fileName: String, containerName: String, index: Int) {
    val stripeIndex = containerLoader.maybeExtractStripeIndex(fileName)
    assertThat(stripeIndex).isNotNull()
    assertThat(stripeIndex!!.containerName).isEqualTo(containerName)
    assertThat(stripeIndex.index).isEqualTo(index)
  }

  @ParameterizedTest(name = "pattern => {0}")
  @ValueSource(
    strings = [
      "BaseLinear_%d.aff4",
      "Base-1-Linear.aff4.%03d",
      "Base-Linear.aff4.%d",
    ]
  )
  fun `get extract valid stripes`(pattern: String) {
    for ((index, stripe) in imagesFileSystem.list("base-linear_striped".toPath()).sortedBy { it.name }.withIndex()) {
      val metadata = imagesFileSystem.metadata(stripe)
      imagesFileSystem.source(stripe).buffer().use { source ->
        val fileName = pattern.formatFile(index)

        workingFileSystem.sink(fileName).buffer().use { sink ->
          source.readFully(sink.buffer, metadata.size!!)
        }
      }
    }

    val imageName = pattern.substringBefore('_').substringBefore('.')

    val expectedContainers = listOf(
      LoadedContainersContext.Container(
        container = Aff4Container(
          containerArn = valueFactory.createArn("aff4://7cbb47d0-b04c-42bc-8c04-87b7782739ad"),
          dataFileSystem = workingFileSystem.openZip(pattern.formatFile(0)),
          metadata = Aff4Container.ToolMetadata("1.0", "Evimetry 2.2.0"),
        ),
        imageName = imageName,
        containerFileSystem = workingFileSystem,
        containerPath = pattern.formatFile(0),
      ),
      LoadedContainersContext.Container(
        container = Aff4Container(
          containerArn = valueFactory.createArn("aff4://51725cd9-3769-4be7-a8ab-94e3ea62bf9a"),
          dataFileSystem = workingFileSystem.openZip(pattern.formatFile(1)),
          metadata = Aff4Container.ToolMetadata("1.0", "Evimetry 2.2.0"),
        ),
        imageName = imageName,
        containerFileSystem = workingFileSystem,
        containerPath = pattern.formatFile(1),
      ),
    )

    val containers = containerLoader.getContainersForImage(workingFileSystem, pattern.formatFile(1))
    assertThat(containers).hasSize(2)

    assertThat(containers[0])
      .`as` { expectedContainers[0].container.containerArn.toString() }
      .isEqual(expectedContainers[0])
    assertThat(containers[1])
      .`as` { expectedContainers[1].container.containerArn.toString() }
      .isEqual(expectedContainers[1])
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

private fun String.formatFile(index: Int): Path {
  return String.format(this, index).toPath()
}
