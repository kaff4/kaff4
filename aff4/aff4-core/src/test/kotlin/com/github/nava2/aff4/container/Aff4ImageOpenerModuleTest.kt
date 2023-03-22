package com.github.nava2.aff4.container

import com.github.nava2.aff4.Aff4BaseStreamModule
import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.Aff4TestModule
import com.github.nava2.aff4.ForImages
import com.github.nava2.aff4.model.Aff4Image
import com.github.nava2.aff4.model.Aff4ImageContext
import com.github.nava2.aff4.model.Aff4ImageOpener
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.dialect.DefaultToolDialect
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.guice.action_scoped.ActionScoped
import com.github.nava2.test.GuiceModule
import com.google.inject.util.Modules
import okio.FileSystem
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Inject
import javax.inject.Provider

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
  @field:ActionScoped
  private lateinit var aff4ModelProvider: Provider<Aff4Model>

  @Inject
  @field:ActionScoped
  private lateinit var aff4ImageProvider: Provider<Aff4Image>

  @Inject
  @field:ActionScoped
  private lateinit var aff4ImageContextProvider: Provider<Aff4ImageContext>

  @Inject
  @field:ActionScoped
  private lateinit var toolDialectProvider: Provider<ToolDialect>

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
