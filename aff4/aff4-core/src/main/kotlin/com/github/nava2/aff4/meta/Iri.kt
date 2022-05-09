package com.github.nava2.aff4.meta

import org.eclipse.rdf4j.model.IRI

data class Iri(
  val namespace: String,
  val localName: String,
) {
  companion object {
    fun parse(namespaces: Namespaces, prefixedName: String): Iri {
      val parts = prefixedName.split(':')
      check(parts.size == 2) { "Invalid prefixedName: $prefixedName" }

      val (namespace, localName) = parts
      return Iri(namespaces.getValue(namespace), localName)
    }

    fun IRI.asIri(namespaces: Namespaces): Iri {
      val mappedNamespace = if (namespace.endsWith("://")) {
        val normalizedNamespace = namespace.substringBeforeLast("://")
        namespaces[normalizedNamespace] ?: namespace
      } else {
        namespace
      }

      return Iri(mappedNamespace, this.localName)
    }
  }
}
