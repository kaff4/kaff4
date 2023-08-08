package net.navatwo.kaff4.io

interface AutoCloseableSourceProvider<out SOURCE : Source> : SourceProvider<SOURCE>, AutoCloseable
