package com.github.nava2.aff4.io

import okio.Source
import okio.Timeout

interface SourceProvider<out SOURCE : Source> {
  fun get(timeout: Timeout = Timeout.NONE): SOURCE

  fun <TRANSFORMED : Source> transform(transformer: Transformer<SOURCE, TRANSFORMED>): SourceProvider<TRANSFORMED> {
    return TransformedSourceProvider(this, transformer)
  }

  fun interface Transformer<in IN : Source, out TRANSFORMED : Source> {
    fun transform(source: IN): TRANSFORMED
  }
}

private class TransformedSourceProvider<IN : Source, TRANSFORMED : Source>(
  private val sourceProvider: SourceProvider<IN>,
  private val transformer: SourceProvider.Transformer<IN, TRANSFORMED>,
) : SourceProvider<TRANSFORMED> {
  override fun get(timeout: Timeout): TRANSFORMED {
    return transformer.transform(sourceProvider.get(timeout))
  }
}
