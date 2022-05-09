package com.github.nava2.aff4.meta

import javax.inject.Inject

sealed interface Aff4Model {

  abstract class Parser(types: List<String>) {
    @Inject
    lateinit var namespaces: Namespaces

    private val types by lazy(LazyThreadSafetyMode.NONE) {
      types.map { Iri.parse(namespaces, it) }.toSet()
    }

    fun tryParse(context: ModelRdfContext): Aff4Model? {
      if (!matchesTypes(context)) return null
      return protectedTryCreate(context)
    }

    abstract fun protectedTryCreate(context: ModelRdfContext): Aff4Model?

    private fun matchesTypes(context: ModelRdfContext): Boolean {
      return types.any { it in context.types }
    }
  }
}
