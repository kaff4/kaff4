package com.github.nava2.aff4.meta

import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.eclipse.rdf4j.model.Literal

sealed interface Hash {
  val hash: ByteString

  data class Sha1(override val hash: ByteString) : Hash
  data class Sha256(override val hash: ByteString) : Hash
  data class Sha512(override val hash: ByteString) : Hash

  data class Md5(override val hash: ByteString) : Hash

  companion object {
    fun fromLiteral(literal: Literal): Hash {
      val hash = literal.label.decodeHex()
      return when (val hashType = literal.datatype.localName) {
        "SHA1" -> Sha1(hash)
        "SHA256" -> Sha256(hash)
        "SHA512" -> Sha512(hash)
        "MD5" -> Md5(hash)
        else -> error("Unsupported hash type: $hashType")
      }
    }
  }
}
