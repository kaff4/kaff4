package com.github.nava2.aff4.streams

import com.github.nava2.aff4.meta.rdf.model.ImageStream
import java.io.InputStream

class Aff4ImageInputStream(
  private val image: ImageStream,
) : InputStream() {
  override fun read(): Int {
    TODO("Not yet implemented")
  }

}