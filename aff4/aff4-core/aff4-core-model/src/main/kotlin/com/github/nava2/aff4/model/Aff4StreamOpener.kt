package com.github.nava2.aff4.model

import com.github.nava2.aff4.io.AutoCloseableSourceProvider
import com.github.nava2.aff4.model.rdf.Aff4Arn
import okio.Source
import java.io.Closeable

interface Aff4StreamOpener : Closeable {
  fun openStream(arn: Aff4Arn): AutoCloseableSourceProvider<Source>
}
