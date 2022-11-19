package com.github.nava2.aff4.model

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.nava2.aff4.container.ContainerScoped
import com.github.nava2.aff4.io.SourceProvider
import com.github.nava2.aff4.io.bounded
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.model.rdf.annotations.RdfModel
import com.github.nava2.aff4.model.rdf.createArn
import com.github.nava2.aff4.rdf.QueryableRdfConnection
import com.github.nava2.aff4.rdf.RdfConnection
import com.github.nava2.aff4.rdf.RdfExecutor
import com.github.nava2.aff4.rdf.io.RdfModelParser
import com.github.nava2.aff4.streams.Aff4StreamLoaderContext
import com.github.nava2.aff4.streams.symbolics.Symbolics
import com.google.inject.TypeLiteral
import okio.Closeable
import okio.Source
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

private const val MAX_OPEN_STREAMS = 20L

@ContainerScoped
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

  private val openStreams: LoadingCache<Aff4Arn, Aff4StreamSourceProvider> = Caffeine.newBuilder()
    .maximumSize(MAX_OPEN_STREAMS)
    .removalListener<Aff4Arn, SourceProvider<Source>> { _, provider, _ ->
      (provider as? Closeable)?.close()
    }
    .build(::loadSourceProvider)

  override fun openStream(arn: Aff4Arn): SourceProvider<Source> {
    check(!closed) { "Closed" }

    val symbolic = symbolics.maybeGetProvider(arn)
    if (symbolic != null) return symbolic

    // We do not hold open offset stream providers as they are just 'views' over a real stream provider
    return if (!arn.isHashDedupe()) {
      openStreams[arn]!!
    } else {
      loadHashDedupedStream(arn)
    }
  }

  private fun loadHashDedupedStream(subject: Aff4Arn): SourceProvider<Source> {
    require(subject.isHashDedupe()) { "Hash streams are not supported via this method." }

    return rdfExecutor.withReadOnlySession { connection: RdfConnection ->
      val statement = connection.queryStatements(subj = subject).use { it.single() }

      val dataStreamOffset = DataStreamOffsetReference.parse(connection.valueFactory, statement.`object` as Aff4Arn)
      val dataStreamProvider = openStream(dataStreamOffset.dataStream)
      dataStreamProvider.bounded(dataStreamOffset.offset, dataStreamOffset.length)
    }
  }

  private fun loadSourceProvider(subject: Aff4Arn): Aff4StreamSourceProvider {
    check(!closed) { "Closed" }

    require(!subject.isHashDedupe()) { "Hash streams are not supported via this method." }

    return rdfExecutor.withReadOnlySession { connection: RdfConnection ->
      val statements = connection.queryStatements(subj = subject).use { it.toList() }

      loadStreamFromRdf(connection, rdfModelParser, subject, statements)
    }
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
      .firstOrNull() { TypeLiteral.get(it.java) in aff4StreamLoaderContexts }
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
