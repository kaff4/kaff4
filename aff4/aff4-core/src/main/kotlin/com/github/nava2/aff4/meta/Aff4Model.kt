package com.github.nava2.aff4.meta

import com.github.nava2.aff4.meta.rdf.NamespacesProvider
import com.github.nava2.aff4.meta.rdf.ScopedConnection
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import javax.inject.Inject
import javax.inject.Provider

sealed interface Aff4Model {

  abstract class Parser<out T : Aff4Model>(
    types: List<String>,
  ) {
    @Inject
    protected lateinit var namespacesProvider: NamespacesProvider

    @Inject
    private lateinit var connectionProvider: Provider<ScopedConnection>

    val types by lazy(LazyThreadSafetyMode.NONE) {
      types.map { namespacesProvider.iriFromTurtle(it) }.toSet()
    }

    fun tryParse(subject: Resource): T? {
      val connection = connectionProvider.get()
      val statements = connection.queryStatements(subj = subject).use { it.toList() }

      return protectedTryCreate(subject, statements)
    }

    abstract fun protectedTryCreate(subject: Resource, statements: List<Statement>): T?
  }
}
