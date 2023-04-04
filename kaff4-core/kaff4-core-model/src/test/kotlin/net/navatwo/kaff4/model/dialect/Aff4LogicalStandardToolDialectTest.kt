package net.navatwo.kaff4.model.dialect

import com.google.inject.util.Modules
import net.navatwo.kaff4.Aff4TestModule
import net.navatwo.kaff4.TestActionScopeModule
import net.navatwo.kaff4.TestToolDialectModule
import net.navatwo.kaff4.model.Aff4Container
import net.navatwo.kaff4.model.rdf.FileImage
import net.navatwo.kaff4.model.rdf.ImageStream
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.model.rdf.TurtleIri.Companion.toTurtleIri
import net.navatwo.kaff4.model.rdf.ZipSegment
import net.navatwo.test.GuiceModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.inject.Inject
import javax.inject.Provider

internal class Aff4LogicalStandardToolDialectTest {
  @GuiceModule
  val module = Modules.combine(
    Aff4TestModule,
    TestActionScopeModule,
    TestToolDialectModule,
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
