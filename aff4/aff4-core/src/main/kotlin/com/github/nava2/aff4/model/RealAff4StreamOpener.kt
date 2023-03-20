package com.github.nava2.aff4.model

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.nava2.aff4.io.AutoCloseableSourceProvider
import com.github.nava2.aff4.io.SourceProvider
import com.github.nava2.aff4.io.bounded
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.model.rdf.annotations.RdfModel
import com.github.nava2.aff4.model.rdf.createAff4Iri
import com.github.nava2.aff4.model.rdf.createArn
import com.github.nava2.aff4.rdf.QueryableRdfConnection
import com.github.nava2.aff4.rdf.RdfConnection
import com.github.nava2.aff4.rdf.RdfExecutor
import com.github.nava2.aff4.rdf.io.RdfModelParser
import com.github.nava2.aff4.streams.Aff4StreamLoaderContext
import com.github.nava2.aff4.streams.symbolics.Symbolics
import com.github.nava2.guice.action_scoped.ActionScoped
import com.google.inject.TypeLiteral
import okio.Closeable
import okio.Source
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.ValueFactory
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaType

private val closeableMethods = setOf(
  AutoCloseable::close.javaMethod,
  Closeable::close.javaMethod,
  java.io.Closeable::close.javaMethod,
)

@ActionScoped
internal class RealAff4StreamOpener @Inject constructor(
  private val rdfExecutor: RdfExecutor,
  private val rdfModelParser: RdfModelParser,
  private val modelKlasses: Set<KClass<out Aff4RdfModel>>,
  aff4StreamLoaderContexts: Set<Aff4StreamLoaderContext>,
  private val symbolics: Symbolics,
) : Aff4StreamOpener {
  @Volatile
  private var closed = false

  private val modelKlassesByRdfType = rdfExecutor.withReadOnlySession { connection ->
    modelKlasses.associateBy { klass ->
      val rdfModelType = klass.findAnnotation<RdfModel>()!!.rdfType
      connection.namespaces.iriFromTurtle(rdfModelType)
    }
  }

  private val aff4StreamLoaderContexts = aff4StreamLoaderContexts.associateBy { it.configTypeLiteral }

  private val openStreams: LoadingCache<Aff4Arn, AutoCloseableSourceProvider<Source>> = Caffeine.newBuilder()
    .weakValues()
    .build(::loadSourceProvider)

  private inner class ProxyCloseHandler(
    private val delegate: Aff4StreamSourceProvider,
  ) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
      return if (method in closeableMethods) {
        openStreams.invalidate(delegate.arn)

        if (delegate is AutoCloseable) {
          delegate.close()
        }

        Unit
      } else {
        if (args == null) {
          method.invoke(delegate)
        } else {
          @Suppress("SpreadOperator") // can't optimize for java call
          method.invoke(delegate, *args)
        }
      }
    }
  }

  override fun openStream(arn: Aff4Arn): AutoCloseableSourceProvider<Source> {
    check(!closed) { "closed" }

    data class WrappedNoOpCloseableSourceProvider(
      private val delegate: SourceProvider<Source>,
    ) : AutoCloseableSourceProvider<Source>, SourceProvider<Source> by delegate {
      override fun close() = Unit

      init {
        require(delegate !is AutoCloseable) {
          "This class is intended to be used with non-closeable values"
        }
      }
    }

    val symbolic = symbolics.maybeGetProvider(arn)
    return when {
      symbolic != null -> WrappedNoOpCloseableSourceProvider(symbolic)
      arn.isHashDedupe() -> WrappedNoOpCloseableSourceProvider(loadHashDedupedStream(arn))
      else -> openStreams[arn]!!
    }
  }

  private fun loadHashDedupedStream(subject: Aff4Arn): SourceProvider<Source> {
    require(subject.isHashDedupe()) { "Hash streams are not supported via this method." }

    return rdfExecutor.withReadOnlySession { connection: RdfConnection ->
      val dataStream = connection.valueFactory.createAff4Iri("dataStream")
      val statement = connection.queryStatements(subj = subject, pred = dataStream).use { it.single() }

      val dataStreamOffset = DataStreamOffsetReference.parse(connection.valueFactory, statement.`object` as Aff4Arn)
      val dataStreamProvider = openStream(dataStreamOffset.dataStream)
      dataStreamProvider.bounded(dataStreamOffset.offset, dataStreamOffset.length)
    }
  }

  private fun loadSourceProvider(subject: Aff4Arn): AutoCloseableSourceProvider<Source> {
    check(!closed) { "closed" }

    require(!subject.isHashDedupe()) { "Hash streams are not supported via this method." }

    val streamSourceProvider = rdfExecutor.withReadOnlySession { connection: RdfConnection ->
      val statements = connection.queryStatements(subj = subject).use { it.toList() }

      loadStreamFromRdf(connection, rdfModelParser, subject, statements)
    }

    val expectedInterfaces = setOf(
      AutoCloseableSourceProvider::class.java,
    )
    val implementedInterfaces = streamSourceProvider::class.getImplementedInterfacesForProxy()

    val proxy = Proxy.newProxyInstance(
      Aff4StreamSourceProvider::class.java.classLoader,
      (implementedInterfaces + expectedInterfaces).toTypedArray(),
      ProxyCloseHandler(streamSourceProvider),
    ) as AutoCloseableSourceProvider<Source>

    return proxy
  }

  private fun loadStreamFromRdf(
    connection: QueryableRdfConnection,
    rdfModelParser: RdfModelParser,
    streamIri: IRI,
    statements: List<Statement>
  ): Aff4StreamSourceProvider {
    val rdfTypes = statements.asSequence()
      .filter { it.predicate == connection.namespaces.iriFromTurtle("rdf:type") }
      .mapNotNull { it.`object` as? Aff4Arn }
      .toSet()

    val modelType = rdfTypes.asSequence().mapNotNull { type -> modelKlassesByRdfType[type] }
      .firstOrNull { TypeLiteral.get(it.java) in aff4StreamLoaderContexts }
      ?: error("Could not load Stream: $streamIri")

    val rdfModel = rdfModelParser.parse(connection, modelType, streamIri, statements)
    val streamLoader = aff4StreamLoaderContexts.getValue(TypeLiteral.get(modelType.java)).get()
    return streamLoader.load(rdfModel)
  }

  override fun close() {
    if (closed) return
    closed = true

    openStreams.invalidateAll()
    openStreams.cleanUp()
  }
}

private data class DataStreamOffsetReference(
  val dataStream: Aff4Arn,
  val offset: Long,
  val length: Long,
) {
  companion object {
    fun parse(valueFactory: ValueFactory, iri: IRI): DataStreamOffsetReference {
      val (dataStream, indexNotation) = iri.stringValue().split("[")
      val (startIndexHex, lengthHex) = indexNotation.substringBeforeLast(']').split(':')
      val offset = startIndexHex.substringAfter("0x").toLong(radix = 16)
      val length = lengthHex.substringAfter("0x").toLong(radix = 16)
      return DataStreamOffsetReference(valueFactory.createArn(dataStream), offset, length)
    }
  }
}

private fun Aff4Arn.isHashDedupe(): Boolean {
  val iriValue = stringValue()
  return iriValue.startsWith("aff4:") &&
    iriValue.endsWith("==") &&
    iriValue.indexOf(':', startIndex = "aff4:".length) != -1
}

private const val INTERFACE_LOOKUP_CACHE_MAX_SIZE = 20L

private val interfacesCache = Caffeine.newBuilder()
  .weakKeys()
  .weakValues()
  .maximumSize(INTERFACE_LOOKUP_CACHE_MAX_SIZE)
  .build<KClass<*>, Set<Class<*>>> { key ->
    key.supertypes.asSequence()
      .filter { type ->
        val klass = type.classifier as? KClass<*>
        klass?.isAbstract == true && klass.constructors.isEmpty()
      }
      .mapNotNull {
        when (val type = it.javaType) {
          is Class<*> -> type
          is ParameterizedType -> type.rawType as Class<*>
          else -> null
        }
      }
      .toSet()
  }

private fun KClass<*>.getImplementedInterfacesForProxy(): Set<Class<out Any>> = interfacesCache[this]!!
