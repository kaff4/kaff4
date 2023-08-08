package net.navatwo.kaff4.io

import okio.Timeout

interface SourceProvider<out SOURCE : Source> {
  fun source(timeout: Timeout): SOURCE = source(position = 0L, timeout)

  fun source(position: Long, timeout: Timeout): SOURCE

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
  override fun source(position: Long, timeout: Timeout): TRANSFORMED {
    return sourceProvider.source(position, timeout)
      .runAndCloseOnThrow { transformer.transform(this) }
  }
}
