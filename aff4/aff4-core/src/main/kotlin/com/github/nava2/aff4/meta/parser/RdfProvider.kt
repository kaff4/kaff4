package com.github.nava2.aff4.meta.parser

import com.github.nava2.aff4.io.SourceProvider
import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.useAsInputStream
import com.github.nava2.aff4.meta.Aff4Model
import com.github.nava2.aff4.meta.rdf.RdfConnectionScoped
import com.github.nava2.aff4.meta.rdf.RdfConnectionScoping
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.getInstance
import com.github.nava2.logging.Logging
import com.google.inject.Injector
import com.google.inject.assistedinject.FactoryModuleBuilder
import okio.Path
import okio.Source
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.rio.RDFFormat
import org.intellij.lang.annotations.Language
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton

private val logger = Logging.getLogger()


@Singleton
class RdfProvider @Inject constructor(
  private val injector: Injector,
  private val rdfConnectionScoping: RdfConnectionScoping,
) {
  fun parseStream(
    imagePath: Path,
    sourceProvider: SourceProvider<Source>,
    consumer: Consumer<Aff4Model>,
  ) {
    rdfConnectionScoping.scoped { parser: Parser ->
      parser.parse(imagePath, sourceProvider)
    }
  }

  @RdfConnectionScoped
  private class Parser @Inject constructor(
    private val connection: RepositoryConnection,
    private val injector: Injector,
  ) {
    fun parse(
      imagePath: Path,
      sourceProvider: SourceProvider<Source>,
    ) {
      sourceProvider.buffer().useAsInputStream { source ->
        connection.add(source, RDFFormat.TURTLE)
      }

      val privateInjector = injector.createChildInjector(
        ScopedParserModule(imagePath),
        object : KAbstractModule() {
          override fun configure() {
            install(
              FactoryModuleBuilder()
                .implement(RdfModelParsingHandler.ParsingCallbacks::class.java, Aff4ModelParsingCallbacks::class.java)
                .build(Aff4ModelParsingCallbacks.Factory::class.java)
            )
          }
        }
      )

      val modelParsers = privateInjector.getInstance<Set<Aff4Model.Parser>>()

      val query = connection.prepareGraphQuery("CONSTRUCT WHERE {?s ?p ?o}")
      query.evaluate().use { result ->
        logger.info("result = ${result.toList()}")
      }

      for (modelParser in modelParsers) {
        @Language("sparql") val queryString = """
            SELECT ?s ?p ?o
            WHERE { 
              ?s a ?type
            }
          """.trimIndent()
        val query = connection.prepareTupleQuery(queryString)
        query.setBinding("type", modelParser.types.single())
        query.evaluate().use { results ->
          for (bindingSet in results) {
            val subject = bindingSet.getValue("subject")

          }
        }
      }
    }
  }
}

