package com.github.nava2.aff4.meta.parser

import com.github.nava2.aff4.meta.Aff4Model
import com.github.nava2.aff4.meta.Iri.Companion.asIri
import com.github.nava2.aff4.meta.ModelRdfContext
import com.github.nava2.aff4.meta.NamespacesProvider
import com.github.nava2.aff4.meta.rdf.RdfConnectionScoped
import com.github.nava2.guice.getValue
import com.github.nava2.logging.Logging
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import java.util.function.Consumer
import javax.inject.Provider

private val logger = Logging.getLogger()

internal class Aff4ModelParsingCallbacks @AssistedInject constructor(
  private val modelParsersProvider: Provider<Set<Aff4Model.Parser>>,
  @RdfConnectionScoped private val namespacesContainer: Provider<NamespacesProvider>,
  @Assisted private val consumer: Consumer<Aff4Model>,
) : RdfModelParsingHandler.ParsingCallbacks {
  val namespaces: NamespacesProvider by namespacesContainer

  override fun onNamespaceDefined(prefix: String, iri: String) {
//    namespaces = namespaces.withNamespace(prefix, iri)
  }

  override fun onModelObjectDefined(subject: Resource, statements: List<Statement>) {
    logger.traceEntry("subject = {}, statements = {}", subject, statements)

    val context = ModelRdfContext(
      namespaces = namespaces,
      iri = (subject as IRI).asIri(namespaces),
      statements = statements,
    )

    val model = modelParsersProvider.get().asSequence().mapNotNull { it.tryParse(context) }.firstOrNull()
    if (model != null) {
      consumer.accept(model)
    }
  }

  interface Factory {
    fun create(consumer: Consumer<Aff4Model>): RdfModelParsingHandler.ParsingCallbacks
  }
}