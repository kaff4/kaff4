package com.github.nava2.aff4.meta

import com.github.nava2.aff4.io.SourceProvider
import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.use
import com.github.nava2.aff4.meta.Iri.Companion.asIri
import com.github.nava2.logging.Logging
import okio.Source
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

val logger = Logging.getLogger()

@Singleton
class RdfProvider @Inject constructor(
  private val modelFactories: @JvmSuppressWildcards Provider<MutableSet<Aff4Model.Factory>>,
) {
  fun parseStream(sourceProvider: SourceProvider<Source>, consumer: Consumer<Aff4Model>) {
    val rdfParser = Rio.createParser(RDFFormat.TURTLE)

    rdfParser.setRDFHandler(ModelParsingHandler { namespaces, subject, statements ->
      logger.traceEntry("namespaces = {}, subject = {}, statements = {}", namespaces, subject, statements)

      val namespaces = Namespaces(namespaces)
      val context = ModelRdfContext(
        namespaces = namespaces,
        iri = (subject as IRI).asIri(namespaces),
        statements = statements,
      )

      val model = modelFactories.get().asSequence().mapNotNull { it.tryCreate(context) }.firstOrNull()
      if (model != null) {
        consumer.accept(model)
      }
    })

    sourceProvider.buffer().use { source ->
      rdfParser.parse(source.inputStream())
    }
  }
}

private class ModelParsingHandler(
  private val callback: (namespaces: Map<String, String>, subject: Resource, statements: List<Statement>) -> Unit,
) : AbstractRDFHandler() {

  private val namespaces = mutableMapOf<String, String>()
  private var currentContext: Context? = null

  override fun endRDF() {
    val currentContext = this.currentContext
    if (currentContext != null) {
      callback(namespaces, currentContext.subject, currentContext.statements)
    }
  }

  override fun handleNamespace(prefix: String, uri: String) {
    logger.traceEntry("handleNamespace: {}, {}", prefix, uri)
    namespaces[prefix] = uri
  }

  override fun handleStatement(st: Statement) {
    logger.traceEntry("handleStatement: {}", st)

    var currentContext = this.currentContext
    if (currentContext?.subject != st.subject!!) {
      if (currentContext != null) {
        callback(namespaces, currentContext.subject, currentContext.statements)
      }

      currentContext = Context(st.subject)
      this.currentContext = currentContext
    }

    currentContext.statements.add(st)
  }

  override fun handleComment(comment: String) {
    logger.traceEntry("handleComment: {}", comment)
  }

  private data class Context(val subject: Resource) {
    val statements = mutableListOf<Statement>()
  }
}