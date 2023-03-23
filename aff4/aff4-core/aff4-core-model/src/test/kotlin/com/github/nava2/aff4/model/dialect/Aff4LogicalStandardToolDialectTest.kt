package com.github.nava2.aff4.model.dialect

import com.github.nava2.aff4.Aff4TestModule
import com.github.nava2.aff4.TestActionScopeModule
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.rdf.FileImage
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.model.rdf.TurtleIri.Companion.toTurtleIri
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.action_scoped.ActionScoped
import com.github.nava2.guice.key
import com.github.nava2.guice.to
import com.github.nava2.test.GuiceModule
import com.google.inject.util.Modules
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.inject.Inject
import javax.inject.Provider

internal class Aff4LogicalStandardToolDialectTest {
  @GuiceModule
  val module = Modules.combine(
    Aff4TestModule,
    TestActionScopeModule,
    Aff4LogicalStandardToolDialect.Module,
    object : KAbstractModule() {
      override fun configure() {
        bind(key<ToolDialect>(DefaultToolDialect::class))
          .to<Aff4LogicalStandardToolDialect>()

        bind<ToolDialect>()
          .to<Aff4LogicalStandardToolDialect>()
          .`in`(ActionScoped::class.java)
      }
    },
  )

  @Inject
  private lateinit var aff4LogicalStandardToolDialect: Aff4LogicalStandardToolDialect

  @Inject
  @field:DefaultToolDialect
  private lateinit var defaultToolDialectProvider: Provider<ToolDialect>

  @Inject
  private lateinit var toolDialectsProvider: Provider<Set<ToolDialect>>

  @Test
  fun `verify correctly maps streams`() {
    val typeResolver = aff4LogicalStandardToolDialect.typeResolver

    assertThat(
      listOf(
        MapStream::class to "aff4:Map".toTurtleIri(),
        ImageStream::class to "aff4:ImageStream".toTurtleIri(),
        ZipSegment::class to "aff4:ZipSegment".toTurtleIri(),
        FileImage::class to "aff4:FileImage".toTurtleIri(),
      )
    ).allSatisfy { (klass, iri) ->
      assertThat(typeResolver[klass]).isEqualTo(iri)
      assertThat(typeResolver[iri]).isEqualTo(klass)
    }
  }

  @Test
  fun `logical standard is always applicable`() {
    val metadatas = listOf(
      Aff4Container.ToolMetadata("foo", "bar"),
      Aff4Container.ToolMetadata("1.0", "pyaff4"),
      Aff4Container.ToolMetadata("0.0.0.04", "definitely-fake"),
    )

    assertThat(metadatas).allSatisfy { metadata ->
      assertThat(aff4LogicalStandardToolDialect.isApplicable(metadata)).isTrue()
    }
  }

  @Test
  fun `@DefaultToolDialect is mapped to Aff4LogicalStandardToolDialect and singleton`() {
    assertThat(defaultToolDialectProvider.get())
      // mapped to logical
      .isSameAs(aff4LogicalStandardToolDialect)
      // singleton
      .isSameAs(defaultToolDialectProvider.get())
  }

  @Test
  fun `Aff4LogicalStandardToolDialect is not injected with other ToolDialect`() {
    assertThat(toolDialectsProvider.get())
      .doesNotContain(aff4LogicalStandardToolDialect)
  }
}
