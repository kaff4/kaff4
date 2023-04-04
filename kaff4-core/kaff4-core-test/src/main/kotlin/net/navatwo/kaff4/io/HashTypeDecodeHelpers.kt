package net.navatwo.kaff4.io

import net.navatwo.kaff4.model.rdf.Hash
import okio.ByteString.Companion.decodeHex

fun Hash.Sha512.Companion.decode(hex: String): Hash.Sha512 {
  return Hash.Sha512(hex.decodeHex())
}

fun Hash.Sha256.Companion.decode(hex: String): Hash.Sha256 {
  return Hash.Sha256(hex.decodeHex())
}

fun Hash.Sha1.Companion.decode(hex: String): Hash.Sha1 {
  return Hash.Sha1(hex.decodeHex())
}

fun Hash.Md5.Companion.decode(hex: String): Hash.Md5 {
  return Hash.Md5(hex.decodeHex())
}
