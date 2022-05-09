package com.github.nava2.aff4.meta

data class Namespaces(private val prefixMap: Map<String, String>) {
  val defaultNamespace: String? = prefixMap[""]

  operator fun get(prefix: String): String? {
    return prefixMap[prefix]
  }

  fun getValue(prefix: String): String {
    return prefixMap.getValue(prefix)
  }

  fun withNamespace(prefix: String, iri: String): Namespaces {
    return copy(prefixMap = prefixMap + mapOf(prefix to iri))
  }
}
