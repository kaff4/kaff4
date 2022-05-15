package com.github.nava2.aff4.streams

import com.github.nava2.aff4.io.concatLazily
import com.github.nava2.aff4.io.source
import com.github.nava2.aff4.meta.rdf.model.Hash
import okio.BufferedSource
import okio.ByteString
import okio.HashingSink
import okio.Sink
import okio.Timeout
import okio.blackholeSink
import okio.buffer
import java.io.Closeable

internal object Hashing {

  fun BufferedSource.computeLinearHashes(linearHashes: List<Hash>): Map<Hash, ByteString> {
    var sinkMap: Map<Hash, HashingSink>? = null

    try {
      var wrappedSink = blackholeSink()

      sinkMap = linearHashes.associateWith { hash ->
        // turtles all the way down
        val hashingSink = hash.hashingSink(wrappedSink)
        wrappedSink = hashingSink
        hashingSink
      }

      wrappedSink.buffer().use { buffer ->
        readAll(buffer)
      }

      wrappedSink.close()

      return sinkMap.mapValues { (_, sink) -> sink.hash }
    } finally {
      for (s in sinkMap?.values ?: listOf()) {
        s.close()
      }
    }
  }

  fun computeConcatHashes(hashes: List<Hash>, sourceProviders: List<() -> BufferedSource>): Map<Hash, ByteString> {
    val hashContextsByHashType = sourceProviders.asSequence()
      .flatMapIndexed { index, sp ->
        var wrappingSink = blackholeSink()
        val contextMap = hashes.associateWith { hash ->
          val context = ConcatHashingContext(hash, wrappingSink)
          wrappingSink = context.hashingSink
          context
        }

        wrappingSink.buffer().use { sink ->
          sp.invoke().use { s -> s.readAll(sink) }
        }

        contextMap.entries.map { it.key to (index to it.value) }
      }
      .groupBy({ (h, _) -> h }) { it.second }
      .mapValues { (_, contexts) -> contexts.sortedBy { it.first }.map { it.second } }

    return hashContextsByHashType.mapValues { (hash, contexts) ->
      concatLazily(contexts.map { { it.hashSource() } }).buffer().use { hashSource ->
        hash.hashingSink().use { hashSink ->
          hashSource.readAll(hashSink)
          hashSink.hash
        }
      }
    }
  }

  private class ConcatHashingContext(hashType: Hash, nextSink: Sink) : Closeable {
    val hashingSink = hashType.hashingSink(nextSink)
    val hash = hashingSink.hash

    fun hashSource(timeout: Timeout = Timeout.NONE) = hash.source(timeout)

    override fun close() {
      hashingSink.close()
    }
  }
}
