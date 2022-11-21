@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.github.nava2.aff4.rdf.io

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.model.rdf.annotations.RdfModel
import com.github.nava2.aff4.model.rdf.annotations.RdfSubject
import com.github.nava2.aff4.model.rdf.annotations.RdfValue
import com.github.nava2.aff4.model.rdf.createArn
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryPlugin
import com.github.nava2.aff4.rdf.MutableRdfConnection
import com.github.nava2.aff4.rdf.RdfExecutor
import com.github.nava2.test.GuiceExtension
import com.github.nava2.test.GuiceModule
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.inject.Inject
import kotlin.Long
import java.lang.Integer as JInteger
import java.lang.Long as JLong

@ExtendWith(GuiceExtension::class)
internal class RdfModelParserTest {
  @GuiceModule
  val modules = listOf(Aff4CoreModule, MemoryRdfRepositoryPlugin)

  @Inject
  lateinit var rdfExecutor: RdfExecutor

  @Inject
  lateinit var rdfModelParser: RdfModelParser

  @Inject
  lateinit var rdfModelSerializer: RdfModelSerializer

  @Test
  fun `primitive properties - long`() {
    data class PrimitiveModelClass @RdfModel("test://primitive-model-class") constructor(
      @RdfSubject
      val subject: Resource,
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

    val expectedModel = PrimitiveModelClass(subject, 100L, 200L as JLong)

    val actualModel = queryModel<PrimitiveModelClass>(subject)

    assertThat(actualModel).isEqualTo(expectedModel)

    verifyRoundTrip(expectedModel)
  }

  @Test
  fun `primitive properties - string`() {
    data class PrimitiveModelClass @RdfModel("test://primitive-model-class") constructor(
      @RdfSubject
      val subject: Resource,
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

    val expectedModel = PrimitiveModelClass(subject, "FooBar")

    val actualModel = queryModel<PrimitiveModelClass>(subject)

    assertThat(actualModel).isEqualTo(expectedModel)

    verifyRoundTrip(expectedModel)
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
      val subject = createArn("test://deadbeef")
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

    verifyRoundTrip(expectedModel)
  }

  @Test
  fun `primitive properties - int`() {
    data class PrimitiveModelClass @RdfModel("test://primitive-model-class") constructor(
      @RdfSubject
      val subject: Resource,
      @RdfValue("test:intValue")
      val intValue: Int,
      @RdfValue("test:jintValue")
      val jintValue: JInteger,
    )

    val subject = setupStatements {
      val subject = createArn("test://deadbeef")
      add(
        subject,
        createIRI("http://test.example.com#intValue") to createLiteral(100),
        createIRI("http://test.example.com#jintValue") to createLiteral(200),
      )

      subject
    }

    val expectedModel = PrimitiveModelClass(subject, 100, 200 as JInteger)

    val actualModel = queryModel<PrimitiveModelClass>(subject)

    assertThat(actualModel).isEqualTo(expectedModel)

    verifyRoundTrip(expectedModel)
  }

  private fun setupStatements(block: SetupFixture.() -> Resource): Resource {
    return rdfExecutor.withReadWriteSession { connection ->
      val fixture = SetupFixture(connection)
      fixture.setNamespace("test", "http://test.example.com#")

      fixture.block()
    }
  }

  private inline fun <reified T : Any> queryModel(subject: Resource): T {
    return rdfExecutor.withReadOnlySession { connection ->
      val statements = connection.queryStatements(subj = subject).use { it.toList() }
      rdfModelParser.parse(connection, T::class, subject, statements)
    }
  }

  private inline fun <reified T : Any> verifyRoundTrip(value: T) {
    return rdfExecutor.withReadWriteSession { connection ->
      val statements = rdfModelSerializer.serialize(connection, value).toList()

      val fromParser = rdfModelParser.parse(connection, T::class, statements.first().subject, statements)
      assertThat(fromParser).isEqualTo(value)
    }
  }

  class SetupFixture(
    private val rdfConnection: MutableRdfConnection,
  ) : ValueFactory by rdfConnection.valueFactory {
    fun add(subject: Resource, vararg values: Pair<IRI, Value>) {
      rdfConnection.add(subject, *values)
    }

    fun setNamespace(prefix: String, name: String) {
      rdfConnection.setNamespace(prefix, name)
    }
  }
}
