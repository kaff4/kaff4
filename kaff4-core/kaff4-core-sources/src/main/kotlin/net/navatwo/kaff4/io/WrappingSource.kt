package net.navatwo.kaff4.io

interface WrappingSource : Source {
  val wrapped: Source
}
