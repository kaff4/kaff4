package net.navatwo.kaff4.container

import com.google.inject.util.Modules
import jakarta.inject.Inject
import misk.scope.ActionScoped
import net.navatwo.kaff4.Aff4BaseStreamModule
import net.navatwo.kaff4.Aff4CoreModule
import net.navatwo.kaff4.Aff4TestModule
import net.navatwo.kaff4.ForImages
import net.navatwo.kaff4.model.Aff4Image
import net.navatwo.kaff4.model.Aff4ImageContext
import net.navatwo.kaff4.model.Aff4ImageOpener
import net.navatwo.kaff4.model.Aff4Model
import net.navatwo.kaff4.model.dialect.DefaultToolDialect
import net.navatwo.kaff4.model.dialect.ToolDialect
import net.navatwo.test.GuiceModule
import okio.FileSystem
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val BASE_LINEAR_NAME = "Base-Linear"
private const val BASE_LINEAR_IMAGE = "$BASE_LINEAR_NAME.aff4"

internal class Aff4ImageOpenerModuleTest {
  @GuiceModule
  val module = Modules.combine(
    Aff4TestModule,
    Aff4ImageOpenerModule,
    Aff4CoreModule,
    Aff4BaseStreamModule,
  )

  @Inject
  private lateinit var aff4ImageOpener: Aff4ImageOpener

  @Inject
  @field:ForImages
  private lateinit var imagesFileSystem: FileSystem

  @Inject
  private lateinit var aff4ModelProvider: ActionScoped<Aff4Model>

  @Inject
  private lateinit var aff4ImageProvider: ActionScoped<Aff4Image>

  @Inject
  private lateinit var aff4ImageContextProvider: ActionScoped<Aff4ImageContext>

  @Inject
  private lateinit var toolDialectProvider: ActionScoped<ToolDialect>

  @Inject
  @field:DefaultToolDialect
  private lateinit var defaultToolDialect: ToolDialect

  private lateinit var aff4ImageWithResources: Aff4ImageOpener.Aff4ImageWithResources

  @BeforeEach
  fun openImage() {
    aff4ImageWithResources = aff4ImageOpener.manualOpen(imagesFileSystem, BASE_LINEAR_IMAGE.toPath())
  }

  @AfterEach
  fun closeImage() {
    aff4ImageWithResources.close()
  }

  @Test
  fun `image context uses expected model`() {
    val imageContext = aff4ImageContextProvider.get()
    assertThat(imageContext.imageName)
      .isEqualTo(BASE_LINEAR_NAME)

    assertThat(imageContext).isSameAs(aff4ImageContextProvider.get())
  }

  @Test
  fun `model has expected image context`() {
    val model = aff4ModelProvider.get()
    assertThat(model.containerContext.imageName)
      .isEqualTo(BASE_LINEAR_NAME)

    assertThat(model).isSameAs(aff4ModelProvider.get())
  }

  @Test
  fun `image uses expected model`() {
    assertThat(aff4ImageProvider.get())
      .isSameAs(aff4ImageWithResources.aff4Image)
      .isSameAs(aff4ImageProvider.get())
  }

  @Test
  fun `tool dialect correctly injects`() {
    val toolDialect = toolDialectProvider.get()
    assertThat(toolDialect)
      .isSameAs(defaultToolDialect)
      .isSameAs(toolDialectProvider.get())
  }
}
