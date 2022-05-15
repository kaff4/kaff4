package com.github.nava2.aff4.meta.rdf.parser

import com.github.nava2.aff4.io.SourceProvider
import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.useAsInputStream
import com.github.nava2.aff4.meta.rdf.RdfConnectionScoped
import com.github.nava2.aff4.meta.rdf.RdfConnectionScoping
import com.github.nava2.aff4.meta.rdf.ScopedConnection
import com.github.nava2.aff4.meta.rdf.io.RdfModel
import com.github.nava2.aff4.meta.rdf.io.RdfModelParser
import com.github.nava2.aff4.meta.rdf.model.Aff4RdfModel
import com.github.nava2.aff4.meta.rdf.querySubjectsByType
import com.github.nava2.guice.getInstance
import com.google.inject.Injector
import okio.Path
import okio.Source
import org.eclipse.rdf4j.rio.RDFFormat
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

@Singleton
class RdfProvider @Inject constructor(
  private val rdfConnectionScoping: RdfConnectionScoping,
) {
  fun parseStream(
    imagePath: Path,
    sourceProvider: SourceProvider<Source>,
    consumer: Consumer<Aff4RdfModel>,
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
      consumer: Consumer<Aff4RdfModel>,
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
  private val models: Provider<Set<KClass<out Aff4RdfModel>>>,
  private val rdfModelParser: RdfModelParser,
  private val connection: ScopedConnection,
) {
  fun parseModels(consumer: Consumer<Aff4RdfModel>) {
    for (modelType in models.get()) {
      val modelRdfType = modelType.findAnnotation<RdfModel>()!!.rdfType
      val subjects = connection.querySubjectsByType(connection.namespaces.iriFromTurtle(modelRdfType))

      for (subject in subjects) {
        val statements = connection.queryStatements(subj = subject).use { it.toList() }
        val model = rdfModelParser.parse(modelType, subject, statements)
        consumer.accept(model)
      }
    }
  }
}
