package net.navatwo.kaff4.streams.symbolics

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.Aff4Schema
import net.navatwo.kaff4.model.rdf.createAff4Iri
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encode
import org.eclipse.rdf4j.model.ValueFactory

// https://github.com/aff4/Standard/blob/master/inprogress/AFF4StandardSpecification-v1.0a.md#44-symbolic-streams
private const val AFF4_SYMBOLIC_CHUNK_BOUNDARY = 1 * 1024 * 1024

private val SPECIAL_STREAM_LOCAL_NAMES = setOf(
  "Zero",
  "UnknownData",
  "UnreadableData",
)

@Singleton
class Symbolics @Inject constructor(
  private val valueFactory: ValueFactory,
) {
  private val loadedSymbolics: MutableMap<Aff4Arn, SymbolicSourceProvider> = mutableMapOf()

  private val byteMapping = mutableMapOf<Byte, Aff4Arn>()

  val zero by lazy { provider(0) }

  fun provider(streamArn: Aff4Arn): SymbolicSourceProvider {
    val normalized = requireNotNull(normalize(streamArn)) {
      "Requested invalid stream: $streamArn"
    }
    return getOrCreate(normalized)
  }

  fun provider(byte: Byte): SymbolicSourceProvider {
    val streamIri = getArnForSimplePattern(byte)
    return provider(streamIri)
  }

  fun getArnForSimplePattern(byte: Byte): Aff4Arn {
    val streamIri = byteMapping.getOrPut(byte) {
      val byteWithPadding = byte.toUByte().toString(radix = 16)
        .padStart(length = 2, '0')
        .uppercase()
      valueFactory.createAff4Iri("SymbolicStream$byteWithPadding")
    }
    return streamIri
  }

  fun maybeGetProvider(streamArn: Aff4Arn): SymbolicSourceProvider? {
    val normalized = normalize(streamArn) ?: return null
    return getOrCreate(normalized)
  }

  private fun normalize(streamArn: Aff4Arn): Aff4Arn? {
    if (streamArn.namespace != Aff4Schema.SCHEMA) return null

    val localName = streamArn.localName
    return when {
      localName == "SymbolicStream00" -> valueFactory.createAff4Iri("Zero")
      localName in SPECIAL_STREAM_LOCAL_NAMES -> streamArn
      localName.startsWith("SymbolicStream") -> streamArn
      else -> null
    }
  }

  private fun getOrCreate(streamArn: Aff4Arn): SymbolicSourceProvider {
    return loadedSymbolics.getOrPut(streamArn) { createSourceProvider(streamArn) }
  }

  private fun createSourceProvider(streamArn: Aff4Arn): SymbolicSourceProvider {
    return when {
      streamArn == valueFactory.createAff4Iri("Zero") ->
        SymbolicSourceProvider(streamArn, ByteString.of(0), AFF4_SYMBOLIC_CHUNK_BOUNDARY)

      streamArn.localName.startsWith("SymbolicStream") -> {
        val byteHex = streamArn.localName.takeLast(2)
        val pattern = byteHex.decodeHex()

        check(pattern.size == 1 && pattern[0] != 0.toByte())
        SymbolicSourceProvider(streamArn, pattern, AFF4_SYMBOLIC_CHUNK_BOUNDARY)
      }

      streamArn == valueFactory.createAff4Iri("UnknownData") ->
        SymbolicSourceProvider(streamArn, "UNKNOWN".encode(Charsets.US_ASCII), AFF4_SYMBOLIC_CHUNK_BOUNDARY)
      streamArn == valueFactory.createAff4Iri("UnreadableData") ->
        SymbolicSourceProvider(streamArn, "UNREADABLEDATA".encode(Charsets.US_ASCII), AFF4_SYMBOLIC_CHUNK_BOUNDARY)

      else -> error("Invalid ARN specified: $streamArn")
    }
  }
}
