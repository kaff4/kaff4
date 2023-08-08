package net.navatwo.kaff4.io

import okio.Source

interface AutoCloseableSourceProvider<out SOURCE : Source> : SourceProvider<SOURCE>, AutoCloseable
