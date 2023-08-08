package net.navatwo.kaff4.model

import net.navatwo.kaff4.io.AutoCloseableSourceProvider
import net.navatwo.kaff4.io.Source
import net.navatwo.kaff4.model.rdf.Aff4Arn
import java.io.Closeable

interface Aff4StreamOpener : Closeable {
  fun openStream(arn: Aff4Arn): AutoCloseableSourceProvider<Source>
}
