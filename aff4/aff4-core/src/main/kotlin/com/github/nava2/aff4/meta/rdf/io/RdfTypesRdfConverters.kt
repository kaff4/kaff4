package com.github.nava2.aff4.meta.rdf.io

import com.github.nava2.guice.typeLiteral
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Value

internal object IriHandler : RdfValueConverter<IRI>(typeLiteral<IRI>()) {
  override fun convert(value: Value): IRI? {
    return value as? IRI
  }
}

internal object ResourceHandler : RdfValueConverter<Resource>(typeLiteral<Resource>()) {
  override fun convert(value: Value): Resource? {
    return value as? Resource
  }
}
