package com.github.nava2.aff4.model

import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.rdf.NamespacesProvider
import com.github.nava2.aff4.rdf.RdfConnectionScoping
import com.github.nava2.aff4.rdf.ScopedConnection
import com.github.nava2.aff4.rdf.io.RdfModel
import com.github.nava2.aff4.rdf.io.RdfModelParser
import com.github.nava2.aff4.streams.Aff4StreamLoaderContext
import com.github.nava2.aff4.streams.BoundedAff4Stream
import com.github.nava2.aff4.streams.symbolics.Symbolics
import com.google.inject.TypeLiteral
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.ValueFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

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
  private val openStreams = ConcurrentHashMap<IRI, Aff4Stream>()

  override fun openStream(iri: IRI): Aff4Stream {
    check(!closed) { "Closed" }

    val symbolic = symbolics.maybeGetProvider(iri)
    if (symbolic != null) return symbolic

    return openStreams.computeIfAbsent(iri) { key ->
      rdfConnectionScoping.scoped { connection: ScopedConnection, rdfModelParser: RdfModelParser ->
        val namespaces = connection.namespaces
        val statements = connection.queryStatements(subj = key).use { it.toList() }

        if (isIriHashDedupe(key)) {
          val statement = statements.single()
          loadHashDedupedStream(connection.valueFactory, statement.`object` as IRI)
        } else {
          loadStreamFromRdf(namespaces, rdfModelParser, iri, statements)
        }
      }
    }
  }

  private fun loadStreamFromRdf(
    namespaces: NamespacesProvider,
    rdfModelParser: RdfModelParser,
    streamIri: IRI,
    statements: List<Statement>
  ): Aff4Stream {
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
  ): Aff4Stream {
    val (dataStreamIri, indexNotation) = obj.stringValue().split("[")
    val (startIndexHex, lengthHex) = indexNotation.substringBeforeLast(']').split(':')
    val startIndex = startIndexHex.substringAfter("0x").toLong(radix = 16)
    val length = lengthHex.substringAfter("0x").toLong(radix = 16)

    val dataStream = openStream(valueFactory.createIRI(dataStreamIri))
    return BoundedAff4Stream(dataStream, startIndex, length)
  }

  override fun close() {
    if (closed) return
    closed = true

    val streamsToClose = openStreams.run {
      val beforeClear = values.toSet()
      clear()
      beforeClear + values.toSet()
    }

    for (stream in streamsToClose) {
      stream.close()
    }
  }
}
