package net.navatwo.kaff4.rdf

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection

private val currentConnection: ThreadLocal<QueryableRdfConnection?> = ThreadLocal.withInitial { null }

@Singleton
internal class RealRdfExecutor @Inject constructor(
  private val realMutableRdfConnectionFactory: RealMutableRdfConnection.Factory,
  private val repository: Repository,
) : RdfExecutor {
  override fun <R> withReadOnlySession(block: (connection: RdfConnection) -> R): R {
    // nested scopes are fun
    return when (val connection = currentConnection.get()) {
      // If we are nesting read-only, just reuse the connection
      is RdfConnection -> block(connection)
      // If we are nesting mutable, we need to wrap it to avoid exposing mutable
      is MutableRdfConnection -> block(NestedRdfConnection(connection))
      else -> executeSessionBlock(block, ::RealRdfConnection)
    }
  }

  override fun <R> withReadWriteSession(block: (connection: MutableRdfConnection) -> R): R {
    return when (val connection = currentConnection.get()) {
      is RdfConnection -> error("Can not promote read-only connection to read-write.")
      // Re-use an existing connection instance
      is MutableRdfConnection -> block(connection)
      else -> executeSessionBlock(block, realMutableRdfConnectionFactory::create)
    }
  }

  private inline fun <T : QueryableRdfConnection, R> executeSessionBlock(
    block: (newConnection: T) -> R,
    factory: (repositoryConnection: RepositoryConnection) -> T,
  ): R {
    return repository.connection.use { repositoryConnection ->
      try {
        val newConnection = factory.invoke(repositoryConnection)
        currentConnection.set(newConnection)
        block(newConnection)
      } finally {
        currentConnection.set(null)
      }
    }
  }

  internal data class NestedRdfConnection(val queryableRdfConnection: QueryableRdfConnection) :
    RdfConnection, QueryableRdfConnection by queryableRdfConnection
}
