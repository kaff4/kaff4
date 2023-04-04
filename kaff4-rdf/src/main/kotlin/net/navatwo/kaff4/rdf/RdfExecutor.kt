package net.navatwo.kaff4.rdf

interface RdfExecutor {
  fun <R> withReadOnlySession(block: (connection: RdfConnection) -> R): R

  fun <R> withReadWriteSession(block: (connection: MutableRdfConnection) -> R): R
}
