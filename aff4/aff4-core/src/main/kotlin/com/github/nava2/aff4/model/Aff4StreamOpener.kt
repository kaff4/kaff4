package com.github.nava2.aff4.model

import com.github.nava2.aff4.meta.rdf.NamespacesProvider
import com.github.nava2.aff4.meta.rdf.RdfConnectionScoping
import com.github.nava2.aff4.meta.rdf.ScopedConnection
import com.github.nava2.aff4.meta.rdf.io.RdfModel
import com.github.nava2.aff4.meta.rdf.io.RdfModelParser
import com.github.nava2.aff4.meta.rdf.model.Aff4RdfModel
import com.github.nava2.aff4.streams.Aff4Stream
import com.github.nava2.aff4.streams.Aff4StreamLoaderContext
import com.github.nava2.aff4.streams.symbolics.Symbolics
import com.google.inject.TypeLiteral
import org.eclipse.rdf4j.model.IRI
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class Aff4StreamOpener @Inject internal constructor(
  private val rdfConnectionScoping: RdfConnectionScoping,
  private val modelKlasses: Set<KClass<out Aff4RdfModel>>,
  aff4StreamLoaderContexts: Set<Aff4StreamLoaderContext>,
  private val symbolics: Symbolics,
) : AutoCloseable {
  @Volatile
  private var closed = false

  private val modelKlassesByRdfType by lazy {
    rdfConnectionScoping.scoped { namespaces: NamespacesProvider ->
      modelKlasses.associateBy { klass ->
        val rdfModelType = klass.findAnnotation<RdfModel>()!!.rdfType
        namespaces.iriFromTurtle(rdfModelType)
      }
    }
  }

  private val aff4StreamLoaderContexts = aff4StreamLoaderContexts.associateBy { it.configTypeLiteral }

  // TODO For logical images, this will be _massive_ and not okay. We will need to be smarter about caching
  private val openStreams = ConcurrentHashMap<IRI, Aff4Stream>()

  fun openStream(iri: IRI): Aff4Stream {
    check(!closed) { "Closed" }

    val symbolic = symbolics.maybeGetProvider(iri)
    if (symbolic != null) return symbolic

    return openStreams.computeIfAbsent(iri) { key ->
      rdfConnectionScoping.scoped { connection: ScopedConnection, rdfModelParser: RdfModelParser ->
        val namespaces = connection.namespaces
        val statements = connection.queryStatements(subj = key).use { it.toList() }
        val rdfTypes = statements.asSequence()
          .filter { it.predicate == namespaces.iriFromTurtle("rdf:type") }
          .mapNotNull { it.`object` as? IRI }
          .toSet()

        val modelType = rdfTypes.asSequence().mapNotNull { type -> modelKlassesByRdfType[type] }
          .first { TypeLiteral.get(it.java) in aff4StreamLoaderContexts }

        val rdfModel = rdfModelParser.parse(modelType, iri, statements)
        val streamLoader = aff4StreamLoaderContexts.getValue(TypeLiteral.get(modelType.java)).get()
        streamLoader.load(rdfModel)
      }
    }
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
