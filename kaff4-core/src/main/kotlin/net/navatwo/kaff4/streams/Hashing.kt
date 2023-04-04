package net.navatwo.kaff4.streams

import net.navatwo.kaff4.model.rdf.HashType
import okio.ByteString
import okio.HashingSink
import okio.Sink
import okio.Source
import okio.blackholeSink
import okio.buffer

fun Source.computeLinearHashes(linearHashTypes: Collection<HashType>): Map<HashType, ByteString> {
  var sinkMap: Map<HashType, HashingSink>? = null

  try {
    var wrappedSink = blackholeSink()

    sinkMap = linearHashTypes.associateWith { hash ->
      // turtles all the way down
      val hashingSink = hash.hashingSink(wrappedSink)
      wrappedSink = hashingSink
      hashingSink
    }

    wrappedSink.buffer().use { buffer ->
      buffer.writeAll(this)
    }

    wrappedSink.close()

    return sinkMap.mapValues { (_, sink) -> sink.hash }
  } finally {
    for (s in sinkMap?.values ?: listOf()) {
      s.close()
    }
  }
}

fun Source.computeLinearHash(linearHashType: HashType): ByteString {
  val linearHashes = computeLinearHashes(listOf(linearHashType))
  return linearHashes.getValue(linearHashType)
}

fun HashType.hashingSink(delegateSink: Sink = blackholeSink()): HashingSink {
  return when (this) {
    HashType.SHA1 -> HashingSink.sha1(delegateSink)
    HashType.MD5 -> HashingSink.md5(delegateSink)
    HashType.SHA256 -> HashingSink.sha256(delegateSink)
    HashType.SHA512 -> HashingSink.sha512(delegateSink)
  }
}
