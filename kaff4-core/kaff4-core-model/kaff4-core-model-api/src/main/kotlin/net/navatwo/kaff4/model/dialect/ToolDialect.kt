package net.navatwo.kaff4.model.dialect

import net.navatwo.kaff4.model.Aff4Container

interface ToolDialect {
  val typeResolver: DialectTypeResolver

  /**
   * Returns true if this dialect applies to the tool
   */
  fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean
}
