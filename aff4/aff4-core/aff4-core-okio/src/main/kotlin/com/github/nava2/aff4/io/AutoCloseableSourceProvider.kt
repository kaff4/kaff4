package com.github.nava2.aff4.io

import okio.Source

interface AutoCloseableSourceProvider<out SOURCE : Source> : SourceProvider<SOURCE>, AutoCloseable
