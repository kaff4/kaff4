package com.github.nava2.aff4.container

import com.github.nava2.aff4.Aff4TestModule
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.dialect.DefaultToolDialect
import com.github.nava2.aff4.model.dialect.DialectTypeResolver
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.key
import com.github.nava2.test.GuiceModule
import com.google.inject.util.Modules
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.inject.Inject

internal class ToolDialectResolverTest {
  @GuiceModule
  val module = Modules.combine(
    Aff4TestModule,
    object : KAbstractModule() {
      override fun configure() {
        bind(key<ToolDialect>(DefaultToolDialect::class))
          .toInstance(DefaultToolDialectImpl)

        bindSet<ToolDialect> {
          toInstance(ToolDialectOne)
          toInstance(ToolDialectTwo)
        }
      }
    }
  )

  @Inject
  private lateinit var toolDialectResolver: ToolDialectResolver

  @Test
  fun `resolver returns correct dialect for metadata`() {
    assertThat(toolDialectResolver.forTool(ToolDialectOne.toolMetadata))
      .isEqualTo(ToolDialectOne)
    assertThat(toolDialectResolver.forTool(ToolDialectTwo.toolMetadata))
      .isEqualTo(ToolDialectTwo)
  }

  @Test
  fun `resolver returns default dialect if none are found`() {
    assertThat(toolDialectResolver.forTool(Aff4Container.ToolMetadata("V", "T")))
      .isEqualTo(DefaultToolDialectImpl)
  }
}

private object ToolDialectOne : ToolDialect {
  val toolMetadata = Aff4Container.ToolMetadata(
    version = "One",
    tool = "One",
  )

  override val typeResolver: DialectTypeResolver
    get() = TODO("Not yet implemented")

  override fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean {
    return toolMetadata == this.toolMetadata
  }
}

private object ToolDialectTwo : ToolDialect {
  val toolMetadata = Aff4Container.ToolMetadata(
    version = "Two",
    tool = "Two",
  )

  override val typeResolver: DialectTypeResolver
    get() = TODO("Not yet implemented")

  override fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean {
    return toolMetadata == this.toolMetadata
  }
}

private object DefaultToolDialectImpl : ToolDialect {
  override val typeResolver: DialectTypeResolver
    get() = TODO("Not yet implemented")

  override fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean {
    error("This should not be called")
  }
}
