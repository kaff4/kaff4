package com.github.nava2.aff4.streams.symbolics

import com.github.nava2.aff4.meta.rdf.createAff4Iri
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encode
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject
import javax.inject.Singleton

// https://github.com/aff4/Standard/blob/master/inprogress/AFF4StandardSpecification-v1.0a.md#44-symbolic-streams
private const val AFF4_SYMBOLIC_CHUNK_BOUNDARY = 1 * 1024 * 1024

@Singleton
class Symbolics @Inject constructor(
  private val valueFactory: ValueFactory,
) {
  private val loadedSymbolics: MutableMap<IRI, SymbolicSourceProvider> = mutableMapOf()

  private val byteMapping = mutableMapOf<Byte, IRI>()

  fun provider(streamIri: IRI): SymbolicSourceProvider {
    return getOrCreate(streamIri)
  }

  fun provider(byte: Byte): SymbolicSourceProvider {
    val streamIri = byteMapping.getOrPut(byte) {
      val byteWithPadding = byte.toUByte().toString(radix = 16)
        .padStart(length = 2, '0')
        .uppercase()
      valueFactory.createAff4Iri("SymbolicStream$byteWithPadding")
    }
    return provider(streamIri)
  }

  private fun getOrCreate(streamIri: IRI): SymbolicSourceProvider {
    return loadedSymbolics.getOrPut(streamIri) {
      when {
        streamIri == valueFactory.createAff4Iri("Zero") ->
          SymbolicSourceProvider(streamIri, ByteString.of(0), AFF4_SYMBOLIC_CHUNK_BOUNDARY)

        streamIri.localName.startsWith("SymbolicStream") -> {
          val byteHex = streamIri.localName.takeLast(2)
          val pattern = byteHex.decodeHex()

          if (pattern[0] == 0.toByte()) {
            getOrCreate(valueFactory.createAff4Iri("Zero"))
          } else {
            SymbolicSourceProvider(streamIri, pattern, AFF4_SYMBOLIC_CHUNK_BOUNDARY)
          }
        }

        streamIri == valueFactory.createAff4Iri("UnknownData") ->
          SymbolicSourceProvider(streamIri, "UNKNOWN".encode(Charsets.US_ASCII), AFF4_SYMBOLIC_CHUNK_BOUNDARY)
        streamIri == valueFactory.createAff4Iri("UnreadableData") ->
          SymbolicSourceProvider(streamIri, "UNREADABLEDATA".encode(Charsets.US_ASCII), AFF4_SYMBOLIC_CHUNK_BOUNDARY)
        else -> error("Unknown iri: $streamIri")
      }
    }
  }
}
