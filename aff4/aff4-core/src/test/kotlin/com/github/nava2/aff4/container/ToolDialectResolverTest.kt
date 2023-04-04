package com.github.nava2.aff4.container

import com.github.nava2.aff4.Aff4TestModule
import com.github.nava2.aff4.TestActionScopeModule
import com.github.nava2.aff4.isInstanceOf
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.dialect.DefaultToolDialect
import com.github.nava2.aff4.model.dialect.DialectTypeResolver
import com.github.nava2.aff4.model.dialect.DialectsModule
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.model.rdf.Aff4RdfModelPlugin
import com.github.nava2.guice.KAff4AbstractModule
import com.github.nava2.guice.action_scoped.ActionScoped
import com.github.nava2.guice.key
import com.github.nava2.test.GuiceModule
import com.google.inject.util.Modules
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal class ToolDialectResolverTest {
  @GuiceModule
  val module = Modules.combine(
    Aff4TestModule,
    TestActionScopeModule,
    Modules.override(DialectsModule).with(CustomDialectModule),
    object : KAff4AbstractModule() {
      override fun configure() {
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
      .isInstanceOf<DefaultToolDialectImpl>()
  }
}

private object ToolDialectOne : ToolDialect {
  val toolMetadata = Aff4Container.ToolMetadata(
    version = "One",
    tool = "One",
  )

  override val typeResolver: DialectTypeResolver = DialectTypeResolver.EMPTY

  override fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean {
    return toolMetadata == this.toolMetadata
  }
}

private object ToolDialectTwo : ToolDialect {
  val toolMetadata = Aff4Container.ToolMetadata(
    version = "Two",
    tool = "Two",
  )

  override val typeResolver: DialectTypeResolver = DialectTypeResolver.EMPTY

  override fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean {
    return toolMetadata == this.toolMetadata
  }
}

@Singleton
private class DefaultToolDialectImpl @Inject constructor() : ToolDialect {

  override val typeResolver: DialectTypeResolver = DialectTypeResolver.EMPTY

  override fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean {
    error("This should not be called")
  }
}

private object CustomDialectModule : KAff4AbstractModule() {
  override fun configure() {
    install(Aff4RdfModelPlugin)

    bind(key<ToolDialect>(DefaultToolDialect::class))
      .to<DefaultToolDialectImpl>()
    bind<ToolDialect>()
      .toProvider(ToolDialectActionScopeProvider::class.java)
      .`in`(ActionScoped::class.java)
  }

  private class ToolDialectActionScopeProvider @Inject constructor(
    private val provider: Provider<DefaultToolDialectImpl>,
  ) : Provider<DefaultToolDialectImpl> {
    override fun get(): DefaultToolDialectImpl = provider.get()
  }
}
