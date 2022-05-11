package com.github.nava2.aff4.meta.parser

import com.github.nava2.aff4.io.SourceProvider
import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.useAsInputStream
import com.github.nava2.aff4.meta.Aff4Model
import com.github.nava2.aff4.meta.rdf.RdfConnectionScoped
import com.github.nava2.aff4.meta.rdf.RdfConnectionScoping
import com.github.nava2.aff4.meta.rdf.ScopedConnection
import com.github.nava2.guice.getInstance
import com.google.inject.Injector
import okio.Path
import okio.Source
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.query.TupleQueryResult
import org.eclipse.rdf4j.rio.RDFFormat
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class RdfProvider @Inject constructor(
  private val rdfConnectionScoping: RdfConnectionScoping,
) {
  fun parseStream(
    imagePath: Path,
    sourceProvider: SourceProvider<Source>,
    consumer: Consumer<Aff4Model>,
  ) {
    rdfConnectionScoping.scoped { parser: Parser ->
      parser.parse(imagePath, sourceProvider, consumer)
    }
  }

  @RdfConnectionScoped
  private class Parser @Inject constructor(
    private val connection: ScopedConnection,
    private val injector: Injector,
  ) {
    fun parse(
      imagePath: Path,
      sourceProvider: SourceProvider<Source>,
      consumer: Consumer<Aff4Model>,
    ) {
      sourceProvider.buffer().useAsInputStream { source ->
        connection.mutable.add(source, RDFFormat.TURTLE)
      }

      val privateInjector = injector.createChildInjector(
        ScopedParserModule(imagePath),
      )

      val modelParsers = privateInjector.getInstance<ModelParsers>()
      modelParsers.parseModels(consumer)
    }
  }
}

private class ModelParsers @Inject constructor(
  private val modelParsers: Provider<Set<Aff4Model.Parser<*>>>,
  private val connection: ScopedConnection,
) {
  fun parseModels(consumer: Consumer<Aff4Model>) {
    for (modelParser in modelParsers.get()) {
      val subjects = connection.querySubjectsByType(modelParser.types.single())

      for (model in subjects.mapNotNull { modelParser.tryParse(it) }) {
        consumer.accept(model)
      }
    }
  }
}

private fun ScopedConnection.querySubjectsByType(type: IRI): List<Resource> {
  val query = queryStatements(pred = namespaces.iriFromTurtle("rdf:type"), obj = type).apply {
    enableDuplicateFilter()
  }
  return query.use { result -> result.map { it.subject } }
}

inline fun <T> TupleQuery.executeQuery(block: (result: TupleQueryResult) -> T): T {
  return evaluate().use { result ->
    block(result)
  }
}
