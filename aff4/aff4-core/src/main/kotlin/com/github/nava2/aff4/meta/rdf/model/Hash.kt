package com.github.nava2.aff4.meta.rdf.model

import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.eclipse.rdf4j.model.Literal

enum class HashType(val byteCount: Long) {
  SHA1(byteCount = 20),
  SHA256(byteCount = 32),
  SHA512(byteCount = 64),
  MD5(byteCount = 16),
  ;

  fun value(hash: ByteString): Hash = when (this) {
    SHA1 -> Hash.Sha1(hash)
    SHA256 -> Hash.Sha256(hash)
    SHA512 -> Hash.Sha512(hash)
    MD5 -> Hash.Md5(hash)
  }
}

sealed class Hash(val hashType: HashType) {
  val name: String
    get() = javaClass.simpleName

  abstract val value: ByteString

  data class Sha1(override val value: ByteString) : Hash(HashType.SHA1)
  data class Sha256(override val value: ByteString) : Hash(HashType.SHA256)
  data class Sha512(override val value: ByteString) : Hash(HashType.SHA512)

  data class Md5(override val value: ByteString) : Hash(HashType.MD5)

  companion object {
    fun fromLiteral(literal: Literal): Hash {
      val hashType = HashType.valueOf(literal.datatype.localName)
      val hash = literal.label.decodeHex()
      return hashType.value(hash)
    }
  }
}
