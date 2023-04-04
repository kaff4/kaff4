package net.navatwo.kaff4.container

import com.google.inject.util.Modules
import misk.scope.ActionScopedProvider
import misk.scope.ActionScopedProviderModule
import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.guice.key
import net.navatwo.kaff4.Aff4TestModule
import net.navatwo.kaff4.TestActionScopeModule
import net.navatwo.kaff4.isInstanceOf
import net.navatwo.kaff4.model.Aff4Container
import net.navatwo.kaff4.model.dialect.DefaultToolDialect
import net.navatwo.kaff4.model.dialect.DialectTypeResolver
import net.navatwo.kaff4.model.dialect.DialectsModule
import net.navatwo.kaff4.model.dialect.ToolDialect
import net.navatwo.kaff4.model.rdf.Aff4RdfModelPlugin
import net.navatwo.test.GuiceModule
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

    install(object : ActionScopedProviderModule() {
      override fun configureProviders() {
        bindProvider(ToolDialect::class, ToolDialectActionScopeProvider::class)
      }
    })
  }

  private class ToolDialectActionScopeProvider @Inject constructor(
    private val provider: Provider<DefaultToolDialectImpl>,
  ) : ActionScopedProvider<DefaultToolDialectImpl> {
    override fun get(): DefaultToolDialectImpl = provider.get()
  }
}
