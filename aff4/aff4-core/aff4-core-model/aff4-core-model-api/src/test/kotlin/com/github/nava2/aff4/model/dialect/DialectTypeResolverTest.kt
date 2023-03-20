package com.github.nava2.aff4.model.dialect

import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.model.rdf.TurtleIri
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DialectTypeResolverTest {
  @Test
  fun `SimpleBuilder - Empty never resolves`() {
    val typeResolver = DialectTypeResolver.Builder.empty().build()

    assertThat(typeResolver[SimpleModel::class]).isNull()
    assertThat(typeResolver[TurtleIri(SIMPLE_IRI)]).isNull()
  }

  @Test
  fun `SimpleBuilder - registered types resolve`() {
    val typeResolver = DialectTypeResolver.Builder.empty()
      .register(SimpleModel::class, SIMPLE_IRI)
      .build()

    assertThat(typeResolver[SimpleModel::class]).isEqualTo(TurtleIri(SIMPLE_IRI))
    assertThat(typeResolver[TurtleIri(SIMPLE_IRI)]).isEqualTo(SimpleModel::class)

    assertThat(typeResolver[AnotherSimpleModel::class]).isNull()
    assertThat(typeResolver[TurtleIri(ANOTHER_IRI)]).isNull()
  }

  @Test
  fun `SimpleBuilder - allows duplicate IRIs, returns primary for KClass key`() {
    val typeResolver = DialectTypeResolver.Builder.empty()
      .register(SimpleModel::class, SIMPLE_IRI, SIMPLE_2_IRI)
      .build()

    assertThat(typeResolver[SimpleModel::class]).isEqualTo(TurtleIri(SIMPLE_IRI))
    assertThat(typeResolver[TurtleIri(SIMPLE_IRI)]).isEqualTo(SimpleModel::class)
    assertThat(typeResolver[TurtleIri(SIMPLE_2_IRI)]).isEqualTo(SimpleModel::class)

    assertThat(typeResolver[AnotherSimpleModel::class]).isNull()
    assertThat(typeResolver[TurtleIri(ANOTHER_IRI)]).isNull()
  }

  @Test
  fun `SimpleBuilder - annotation-based allows duplicate IRIs, returns primary for KClass key`() {
    val typeResolver = DialectTypeResolver.Builder.forAnnotation(TestRdfModel::class) {
      primaryType to rdfTypes.toSet()
    }
      .register<SimpleModel>()
      .register<AnotherSimpleModel>()
      .build()

    assertThat(typeResolver[SimpleModel::class]).isEqualTo(TurtleIri(SIMPLE_IRI))
    assertThat(typeResolver[TurtleIri(SIMPLE_IRI)]).isEqualTo(SimpleModel::class)
    assertThat(typeResolver[TurtleIri(SIMPLE_2_IRI)]).isEqualTo(SimpleModel::class)

    assertThat(typeResolver[AnotherSimpleModel::class]).isEqualTo(TurtleIri(ANOTHER_IRI))
    assertThat(typeResolver[TurtleIri(ANOTHER_IRI)]).isEqualTo(AnotherSimpleModel::class)
  }
}

private const val SIMPLE_IRI = "aff4:Simple"
private const val SIMPLE_2_IRI = "aff4:simple_2"

@TestRdfModel(SIMPLE_IRI, SIMPLE_2_IRI)
private data class SimpleModel(override val arn: Aff4Arn) : Aff4RdfModel

private const val ANOTHER_IRI = "aff4:AnotherSimple"

@TestRdfModel(ANOTHER_IRI)
private data class AnotherSimpleModel(override val arn: Aff4Arn) : Aff4RdfModel

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestRdfModel(val primaryType: String, vararg val rdfTypes: String)
