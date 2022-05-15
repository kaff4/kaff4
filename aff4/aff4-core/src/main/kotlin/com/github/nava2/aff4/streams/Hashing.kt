package com.github.nava2.aff4.streams

import com.github.nava2.aff4.meta.rdf.model.Hash
import okio.BufferedSource
import okio.ByteString
import okio.HashingSink
import okio.blackholeSink
import okio.buffer

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
}
