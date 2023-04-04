package net.navatwo.kaff4.model

import java.io.Closeable

interface Aff4Image : Closeable {
  val aff4Model: Aff4Model
  val streamOpener: Aff4StreamOpener

  val containers: List<Aff4Container>
}
