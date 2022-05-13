package com.github.nava2.aff4.meta.rdf.io

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.meta.parser.TestRdfRepositoryModule
import com.github.nava2.aff4.meta.rdf.RdfConnectionScoping
import com.github.nava2.aff4.meta.rdf.ScopedConnection
import com.github.nava2.test.GuiceTestRule
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import java.lang.Integer as JInteger
import java.lang.Long as JLong

class RdfModelParserTest {
  @get:Rule
  val rule: GuiceTestRule = GuiceTestRule(Aff4CoreModule, TestRdfRepositoryModule)

  @Inject
  private lateinit var rdfConnectionScoping: RdfConnectionScoping

  @Test
  fun `primitive properties - long`() {
    data class PrimitiveModelClass @RdfModel("test://primitive-model-class") constructor(
      @RdfValue("test:longValue")
      val longValue: Long,
      @RdfValue("test:jlongValue")
      val jlongValue: JLong,
    )

    val subject = setupStatements {
      val subject = createIRI("test://deadbeef")
      add(
        subject = subject,
        createIRI("http://test.example.com#longValue") to createLiteral(100L),
        createIRI("http://test.example.com#jlongValue") to createLiteral(200L),
      )

      subject
    }

    val expectedModel = PrimitiveModelClass(100L, 200L as JLong)

    val actualModel = queryModel<PrimitiveModelClass>(subject)

    assertThat(actualModel).isEqualTo(expectedModel)
  }

  @Test
  fun `primitive properties - string`() {
    data class PrimitiveModelClass @RdfModel("test://primitive-model-class") constructor(
      @RdfValue("test:stringValue")
      val stringValue: String,
    )

    val subject = setupStatements {
      val subject = createIRI("test://deadbeef")
      add(
        subject,
        createIRI("http://test.example.com#stringValue") to createLiteral("FooBar"),
      )

      subject
    }

    val expectedModel = PrimitiveModelClass("FooBar")

    val actualModel = queryModel<PrimitiveModelClass>(subject)

    assertThat(actualModel).isEqualTo(expectedModel)
  }

  interface WithSubject {
    @RdfSubject
    val subject: Resource

    @RdfValue("invalid:intValue")
    val intValue: Int
  }

  interface ExtendWithSubject : WithSubject {
    @RdfValue("test:intValue")
    override val intValue: Int
  }

  @Test
  fun `inherit annotations`() {
    data class PrimitiveModelClass @RdfModel("test://primitive-model-class") constructor(
      override val subject: Resource,
      override val intValue: Int,
    ) : ExtendWithSubject

    val subject = setupStatements {
      val subject = createIRI("test://deadbeef")
      add(
        subject,
        createIRI("http://test.example.com#intValue") to createLiteral(100),
        createIRI("http://test.example.com#jintValue") to createLiteral(200),
      )

      subject
    }

    val expectedModel = PrimitiveModelClass(subject, 100)

    val actualModel = queryModel<PrimitiveModelClass>(subject)

    assertThat(actualModel).isEqualTo(expectedModel)
  }

  @Test
  fun `primitive properties - int`() {
    data class PrimitiveModelClass @RdfModel("test://primitive-model-class") constructor(
      @RdfValue("test:intValue")
      val intValue: Int,
      @RdfValue("test:jintValue")
      val jintValue: JInteger,
    )

    val subject = setupStatements {
      val subject = createIRI("test://deadbeef")
      add(
        subject,
        createIRI("http://test.example.com#intValue") to createLiteral(100),
        createIRI("http://test.example.com#jintValue") to createLiteral(200),
      )

      subject
    }

    val expectedModel = PrimitiveModelClass(100, 200 as JInteger)

    val actualModel = queryModel<PrimitiveModelClass>(subject)

    assertThat(actualModel).isEqualTo(expectedModel)
  }

  private fun setupStatements(block: SetupFixture.() -> Resource): Resource {
    return rdfConnectionScoping.scoped { fixture: SetupFixture ->
      fixture.setNamespace("test", "http://test.example.com#")

      fixture.block()
    }
  }

  private inline fun <reified T : Any> queryModel(subject: Resource): T {
    return rdfConnectionScoping.scoped { conn: ScopedConnection, rdfModelParser: RdfModelParser ->
      val statements = conn.queryStatements(subj = subject).use { it.toList() }
      rdfModelParser.parse(T::class, subject, statements)
    }
  }

  class SetupFixture @Inject constructor(
    private val valueFactory: ValueFactory,
    private val scopedConnection: ScopedConnection,
  ) : ValueFactory by valueFactory {
    fun add(subject: Resource, vararg values: Pair<IRI, Value>) {
      scopedConnection.mutable.add(subject, *values)
    }

    fun setNamespace(prefix: String, name: String) {
      scopedConnection.mutable.setNamespace(prefix, name)
    }
  }
}
