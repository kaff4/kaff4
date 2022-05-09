package com.github.nava2.aff4.meta.parser

import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler

internal class RdfModelParsingHandler(
  private val callbacks: ParsingCallbacks,
) : AbstractRDFHandler() {
  private var currentContext: Context? = null

  override fun endRDF() {
    val currentContext = this.currentContext
    if (currentContext != null) {
      callbacks.onModelObjectDefined(currentContext.subject, currentContext.statements)
    }
  }

  override fun handleNamespace(prefix: String, uri: String) {
    logger.traceEntry("handleNamespace: {}, {}", prefix, uri)
    callbacks.onNamespaceDefined(prefix, uri)
  }

  override fun handleStatement(st: Statement) {
    logger.traceEntry("handleStatement: {}", st)

    var currentContext = this.currentContext
    if (currentContext?.subject != st.subject!!) {
      if (currentContext != null) {
        callbacks.onModelObjectDefined(currentContext.subject, currentContext.statements)
      }

      currentContext = Context(st.subject)
      this.currentContext = currentContext
    }

    currentContext.statements.add(st)
  }

  override fun handleComment(comment: String) {
    logger.traceEntry("handleComment: {}", comment)
  }

  interface ParsingCallbacks {
    fun onNamespaceDefined(prefix: String, iri: String)
    fun onModelObjectDefined(subject: Resource, statements: List<Statement>)
  }

  private data class Context(val subject: Resource) {
    val statements = mutableListOf<Statement>()
  }
}