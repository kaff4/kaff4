package com.github.nava2.aff4.meta

import com.github.nava2.aff4.meta.rdf.RdfConnectionScoped
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject

sealed interface Aff4Model {

  abstract class Parser(
    types: List<String>,
  ) {
    @Inject
    private lateinit var valueFactory: ValueFactory

    @Inject
    private lateinit var namespacesProvider: NamespacesProvider

    val types by lazy(LazyThreadSafetyMode.NONE) {
      types.map { type ->
        val (prefix, localName) = type.split(':')
        valueFactory.createIRI(namespacesProvider.fromPrefix(prefix), localName)
      }.toSet()
    }

    fun tryParse(context: ModelRdfContext): Aff4Model? {
//      if (!matchesTypes(context)) return null
      return protectedTryCreate(context)
    }

    abstract fun protectedTryCreate(context: ModelRdfContext): Aff4Model?

//    private fun matchesTypes(context: ModelRdfContext): Boolean {
//      return types.any { it in context.types }
//    }
  }
}
