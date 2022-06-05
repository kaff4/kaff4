package com.github.nava2.aff4.model

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.nava2.aff4.io.SourceProvider
import com.github.nava2.aff4.io.bounded
import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.model.rdf.createArn
import com.github.nava2.aff4.rdf.NamespacesProvider
import com.github.nava2.aff4.rdf.RdfConnectionScoping
import com.github.nava2.aff4.rdf.ScopedConnection
import com.github.nava2.aff4.rdf.io.RdfModel
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
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

private const val MAX_OPEN_STREAMS = 20L

@Singleton
internal class RealAff4StreamOpener @Inject constructor(
  private val rdfConnectionScoping: RdfConnectionScoping,
  private val modelKlasses: Set<KClass<out Aff4RdfModel>>,
  aff4StreamLoaderContexts: Set<Aff4StreamLoaderContext>,
  private val symbolics: Symbolics,
) : Aff4StreamOpener {
  @Volatile
  private var closed = false

  private val modelKlassesByRdfType = rdfConnectionScoping.scoped { namespaces: NamespacesProvider ->
    modelKlasses.associateBy { klass ->
      val rdfModelType = klass.findAnnotation<RdfModel>()!!.rdfType
      namespaces.iriFromTurtle(rdfModelType)
    }
  }

  private val aff4StreamLoaderContexts = aff4StreamLoaderContexts.associateBy { it.configTypeLiteral }

  // TODO For logical images, this will be _massive_ and not okay. We will need to be smarter about caching
  private val openStreams: LoadingCache<IRI, SourceProvider<Source>> = Caffeine.newBuilder()
    .maximumSize(MAX_OPEN_STREAMS)
    .removalListener<IRI, SourceProvider<Source>> { _, provider, _ ->
      (provider as? Closeable)?.close()
    }
    .build(::loadSourceProvider)

  override fun openStream(iri: IRI): SourceProvider<Source> {
    check(!closed) { "Closed" }

    val symbolic = symbolics.maybeGetProvider(iri)
    if (symbolic != null) return symbolic

    // We do not hold open offset stream providers as they are just 'views' over a real stream provider
    return if (!isIriHashDedupe(iri)) {
      openStreams[iri]!!
    } else {
      loadSourceProvider(iri)
    }
  }

  private fun loadSourceProvider(subject: IRI): SourceProvider<Source> {
    return rdfConnectionScoping.scoped { connection: ScopedConnection, rdfModelParser: RdfModelParser ->
      val namespaces = connection.namespaces
      val statements = connection.queryStatements(subj = subject).use { it.toList() }

      if (isIriHashDedupe(subject)) {
        val statement = statements.single()
        loadHashDedupedStream(connection.valueFactory, statement.`object` as IRI)
      } else {
        loadStreamFromRdf(namespaces, rdfModelParser, subject, statements)
      }
    }
  }

  private fun loadStreamFromRdf(
    namespaces: NamespacesProvider,
    rdfModelParser: RdfModelParser,
    streamIri: IRI,
    statements: List<Statement>
  ): Aff4StreamSourceProvider {
    val rdfTypes = statements.asSequence()
      .filter { it.predicate == namespaces.iriFromTurtle("rdf:type") }
      .mapNotNull { it.`object` as? IRI }
      .toSet()

    val modelType = rdfTypes.asSequence().mapNotNull { type -> modelKlassesByRdfType[type] }
      .first { TypeLiteral.get(it.java) in aff4StreamLoaderContexts }

    val rdfModel = rdfModelParser.parse(modelType, streamIri, statements)
    val streamLoader = aff4StreamLoaderContexts.getValue(TypeLiteral.get(modelType.java)).get()
    return streamLoader.load(rdfModel)
  }

  private fun isIriHashDedupe(key: IRI): Boolean {
    val iriValue = key.stringValue()
    return iriValue.startsWith("aff4:") &&
      iriValue.endsWith("==") &&
      iriValue.indexOf(':', startIndex = "aff4:".length) != -1
  }

  private fun loadHashDedupedStream(
    valueFactory: ValueFactory,
    obj: IRI,
  ): SourceProvider<Source> {
    val (dataStreamIri, indexNotation) = obj.stringValue().split("[")
    val (startIndexHex, lengthHex) = indexNotation.substringBeforeLast(']').split(':')
    val startIndex = startIndexHex.substringAfter("0x").toLong(radix = 16)
    val length = lengthHex.substringAfter("0x").toLong(radix = 16)

    val dataStreamProvider = openStream(valueFactory.createArn(dataStreamIri))
    return dataStreamProvider.bounded(startIndex, length)
  }

  override fun close() {
    if (closed) return
    closed = true

    openStreams.invalidateAll()
    openStreams.cleanUp()
  }
}
