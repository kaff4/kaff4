@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package net.navatwo.kaff4.rdf.io

import com.google.inject.util.Modules
import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton
import misk.scope.ActionScopedProvider
import misk.scope.ActionScopedProviderModule
import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.guice.key
import net.navatwo.kaff4.TestActionScopeModule
import net.navatwo.kaff4.model.Aff4Container
import net.navatwo.kaff4.model.dialect.DefaultToolDialect
import net.navatwo.kaff4.model.dialect.DialectTypeResolver
import net.navatwo.kaff4.model.dialect.DialectsModule
import net.navatwo.kaff4.model.dialect.ToolDialect
import net.navatwo.kaff4.model.rdf.Aff4RdfModelFeatureModule
import net.navatwo.kaff4.model.rdf.annotations.RdfSubject
import net.navatwo.kaff4.model.rdf.annotations.RdfValue
import net.navatwo.kaff4.model.rdf.createArn
import net.navatwo.kaff4.rdf.MemoryRdfRepositoryFeatureModule
import net.navatwo.kaff4.rdf.MutableRdfConnection
import net.navatwo.kaff4.rdf.RdfExecutor
import net.navatwo.test.GuiceModule
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.Long
import java.lang.Integer as JInteger
import java.lang.Long as JLong

internal class RdfModelParserTest {
  @GuiceModule
  val module = Modules.combine(
    MemoryRdfRepositoryFeatureModule,
    RdfModelParserModule,
    TestActionScopeModule,
    Modules.override(DialectsModule).with(CustomDialectModule),
  )

  @Inject
  lateinit var rdfExecutor: RdfExecutor

  @Inject
  lateinit var rdfModelParser: RdfModelParser

  @Inject
  private lateinit var rdfModelSerializerFactory: RdfModelSerializer.Factory

  @Inject
  @field:DefaultToolDialect
  private lateinit var defaultToolDialect: ToolDialect

  private lateinit var rdfModelSerializer: RdfModelSerializer

  @BeforeEach
  fun setup() {
    rdfModelSerializer = rdfModelSerializerFactory.create(defaultToolDialect)
  }

  @Test
  fun `primitive properties - long`() {
    val subject = setupStatements {
      val subject = createIRI("test://deadbeef")
      add(
        subject = subject,
        createIRI("http://test.example.com#longValue") to createLiteral(100L),
        createIRI("http://test.example.com#jlongValue") to createLiteral(200L),
      )

      subject
    }

    val expectedModel = PrimitiveModelWithLong(subject, 100L, 200L as JLong)

    val actualModel = queryModel<PrimitiveModelWithLong>(subject)

    assertThat(actualModel).isEqualTo(expectedModel)

    verifyRoundTrip(expectedModel)
  }

  @Test
  fun `primitive properties - string`() {
    val subject = setupStatements {
      val subject = createIRI("test://deadbeef")
      add(
        subject,
        createIRI("http://test.example.com#stringValue") to createLiteral("FooBar"),
      )

      subject
    }

    val expectedModel = PrimitiveModelWithString(subject, "FooBar")

    val actualModel = queryModel<PrimitiveModelWithString>(subject)

    assertThat(actualModel).isEqualTo(expectedModel)

    verifyRoundTrip(expectedModel)
  }

  @Test
  fun `inherit annotations`() {
    val subject = setupStatements {
      val subject = createArn("test://deadbeef")
      add(
        subject,
        createIRI("http://test.example.com#intValue") to createLiteral(100),
        createIRI("http://test.example.com#jintValue") to createLiteral(200),
      )

      subject
    }

    val expectedModel = ModelClassExtendWithSubject(subject, 100)

    val actualModel = queryModel<ModelClassExtendWithSubject>(subject)

    assertThat(actualModel).isEqualTo(expectedModel)

    verifyRoundTrip(expectedModel)
  }

  @Test
  fun `primitive properties - int`() {
    val subject = setupStatements {
      val subject = createArn("test://deadbeef")
      add(
        subject,
        createIRI("http://test.example.com#intValue") to createLiteral(100),
        createIRI("http://test.example.com#jintValue") to createLiteral(200),
      )

      subject
    }

    val expectedModel = PrimitiveModelWithInt(subject, 100, 200 as JInteger)

    val actualModel = queryModel<PrimitiveModelWithInt>(subject)

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

@Singleton
private class CustomToolDialect @Inject constructor() : ToolDialect {
  override val typeResolver: DialectTypeResolver = DialectTypeResolver.Builder.newBuilder()
    .register(ModelClassExtendWithSubject::class, "test://extend-with-subject")
    .register(PrimitiveModelWithInt::class, "test://primitive-model-class")
    .register(PrimitiveModelWithString::class, "test://primitive-model-with-string")
    .register(PrimitiveModelWithLong::class, "test://primitive-model-with-long")
    .build()

  override fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean = true
}

internal data class PrimitiveModelWithLong(
  @RdfSubject
  val subject: Resource,
  @RdfValue("test:longValue")
  val longValue: Long,
  @RdfValue("test:jlongValue")
  val jlongValue: JLong,
)

internal data class PrimitiveModelWithString(
  @RdfSubject
  val subject: Resource,
  @RdfValue("test:stringValue")
  val stringValue: String,
)

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

internal data class ModelClassExtendWithSubject(
  override val subject: Resource,
  override val intValue: Int,
) : ExtendWithSubject

internal data class PrimitiveModelWithInt(
  @RdfSubject
  val subject: Resource,
  @RdfValue("test:intValue")
  val intValue: Int,
  @RdfValue("test:jintValue")
  val jintValue: JInteger,
)

private object CustomDialectModule : KAff4AbstractModule() {
  override fun configure() {
    install(Aff4RdfModelFeatureModule)

    bind(key<ToolDialect>(DefaultToolDialect::class))
      .to<CustomToolDialect>()

    install(object : ActionScopedProviderModule() {
      override fun configureProviders() {
        bindProvider(ToolDialect::class, ToolDialectActionScopeProvider::class)
      }
    })
  }

  private class ToolDialectActionScopeProvider @Inject constructor(
    private val provider: Provider<CustomToolDialect>,
  ) : ActionScopedProvider<CustomToolDialect> {
    override fun get(): CustomToolDialect = provider.get()
  }
}
