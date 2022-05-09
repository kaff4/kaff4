package com.github.nava2.aff4.meta

sealed interface Aff4Model {

  abstract class Factory(private val types: List<String>) {

    fun tryCreate(context: ModelRdfContext): Aff4Model? {
      if (!matchesTypes(context)) return null
      return protectedTryCreate(context)
    }

    abstract fun protectedTryCreate(context: ModelRdfContext): Aff4Model?

    private fun matchesTypes(context: ModelRdfContext): Boolean {
      return types.any { Iri.parse(context.namespaces, it) in context.types }
    }
  }
}
