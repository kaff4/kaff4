package com.github.nava2.aff4.meta

import com.github.nava2.aff4.meta.rdf.RdfConnectionScoped
import org.eclipse.rdf4j.repository.RepositoryConnection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NamespacesProvider @Inject constructor(
  @RdfConnectionScoped private val connection: RepositoryConnection,
) {
  private val prefixMap: Map<String, String>
    get() = connection.namespaces.use { iter ->
      iter.associate { it.prefix to it.name }
    }

  val defaultNamespace: String? = prefixMap[""]

  operator fun get(prefix: String): String? {
    return prefixMap[prefix]
  }

  fun fromPrefix(prefix: String): String {
    return prefixMap.getValue(prefix)
  }
}
