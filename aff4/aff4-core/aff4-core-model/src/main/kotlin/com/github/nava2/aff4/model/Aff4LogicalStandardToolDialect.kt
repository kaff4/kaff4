package com.github.nava2.aff4.model

import com.github.nava2.aff4.model.dialect.DialectTypeResolver
import com.github.nava2.aff4.model.dialect.ToolDialect
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

@Singleton
internal class Aff4LogicalStandardToolDialect(
  override val typeResolver: DialectTypeResolver,
) : ToolDialect {

  override fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean {
    return true
  }

  @Target(CLASS)
  @Retention(RUNTIME)
  annotation class RdfStandardType(val rdfType: String)
}
