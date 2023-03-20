package com.github.nava2.aff4.model

import com.github.nava2.aff4.Aff4TestModule
import com.github.nava2.aff4.model.dialect.DefaultToolDialect
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.model.rdf.FileImage
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.model.rdf.TurtleIri
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.test.GuiceModule
import com.google.inject.util.Modules
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.junit.jupiter.api.Test
import javax.inject.Inject
import javax.inject.Provider

internal class Aff4LogicalStandardToolDialectTest {
  @GuiceModule
  val module = Modules.combine(
    Aff4TestModule,
    object : KAbstractModule() {
      override fun configure() {
        bind<ValueFactory>().toInstance(SimpleValueFactory.getInstance())
      }
    }
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
        MapStream::class to TurtleIri("aff4:Map"),
        ImageStream::class to TurtleIri("aff4:ImageStream"),
        ZipSegment::class to TurtleIri("aff4:ZipSegment"),
        FileImage::class to TurtleIri("aff4:FileImage"),
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
