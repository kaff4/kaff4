package com.github.nava2.aff4.meta

@JvmInline
value class Namespaces(private val prefixMap: Map<String, String>) {
  val defaultNamespace: String?
    get() = prefixMap[""]

  operator fun get(prefix: String): String? {
    return prefixMap[prefix]
  }

  fun getValue(prefix: String): String {
    return prefixMap.getValue(prefix)
  }
}