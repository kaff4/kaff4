package com.github.nava2.aff4.meta

import org.eclipse.rdf4j.model.IRI

enum class HashType {
  SHA1,
  MD5,
  SHA256,
  SHA512,
  ;

  companion object {
    fun fromDataType(iri: IRI): HashType {
      return valueOf(iri.localName)
    }
  }
}