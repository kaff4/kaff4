package com.github.nava2.aff4.streams.symbolics

import com.github.nava2.aff4.meta.rdf.createAff4Iri
import com.github.nava2.aff4.model.rdf.Aff4Schema
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encode
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject
import javax.inject.Singleton

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
  private val loadedSymbolics: MutableMap<IRI, SymbolicSourceProvider> = mutableMapOf()

  private val byteMapping = mutableMapOf<Byte, IRI>()

  val zero by lazy { provider(0) }

  fun provider(streamIri: IRI): SymbolicSourceProvider {
    val normalized = requireNotNull(normalize(streamIri)) {
      "Requested invalid stream: $streamIri"
    }
    return getOrCreate(normalized)
  }

  fun provider(byte: Byte): SymbolicSourceProvider {
    val streamIri = getArnForSimplePattern(byte)
    return provider(streamIri)
  }

  fun getArnForSimplePattern(byte: Byte): IRI {
    val streamIri = byteMapping.getOrPut(byte) {
      val byteWithPadding = byte.toUByte().toString(radix = 16)
        .padStart(length = 2, '0')
        .uppercase()
      valueFactory.createAff4Iri("SymbolicStream$byteWithPadding")
    }
    return streamIri
  }

  fun maybeGetProvider(streamIri: IRI): SymbolicSourceProvider? {
    val normalized = normalize(streamIri) ?: return null
    return getOrCreate(normalized)
  }

  private fun normalize(streamIri: IRI): IRI? {
    if (streamIri.namespace != Aff4Schema.SCHEMA) return null

    val localName = streamIri.localName
    return when {
      localName == "SymbolicStream00" -> valueFactory.createAff4Iri("Zero")
      localName in SPECIAL_STREAM_LOCAL_NAMES -> streamIri
      localName.startsWith("SymbolicStream") -> streamIri
      else -> null
    }
  }

  private fun getOrCreate(streamIri: IRI): SymbolicSourceProvider {
    return loadedSymbolics.getOrPut(streamIri) { createSourceProvider(streamIri) }
  }

  private fun createSourceProvider(streamIri: IRI): SymbolicSourceProvider {
    return when {
      streamIri == valueFactory.createAff4Iri("Zero") ->
        SymbolicSourceProvider(streamIri, ByteString.of(0), AFF4_SYMBOLIC_CHUNK_BOUNDARY)

      streamIri.localName.startsWith("SymbolicStream") -> {
        val byteHex = streamIri.localName.takeLast(2)
        val pattern = byteHex.decodeHex()

        check(pattern.size == 1 && pattern[0] != 0.toByte())
        SymbolicSourceProvider(streamIri, pattern, AFF4_SYMBOLIC_CHUNK_BOUNDARY)
      }

      streamIri == valueFactory.createAff4Iri("UnknownData") ->
        SymbolicSourceProvider(streamIri, "UNKNOWN".encode(Charsets.US_ASCII), AFF4_SYMBOLIC_CHUNK_BOUNDARY)
      streamIri == valueFactory.createAff4Iri("UnreadableData") ->
        SymbolicSourceProvider(streamIri, "UNREADABLEDATA".encode(Charsets.US_ASCII), AFF4_SYMBOLIC_CHUNK_BOUNDARY)

      else -> error("Invalid IRI specified: $streamIri")
    }
  }
}
