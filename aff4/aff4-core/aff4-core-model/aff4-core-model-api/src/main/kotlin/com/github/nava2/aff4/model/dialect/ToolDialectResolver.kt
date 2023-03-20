package com.github.nava2.aff4.model.dialect

import com.github.nava2.aff4.model.Aff4Container
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

// TODO simple tests
@Singleton
class ToolDialectResolver @Inject internal constructor(
  private val toolDialects: Set<ToolDialect>,
  @DefaultToolDialect private val defaultToolDialectProvider: Provider<ToolDialect>,
) {
  fun forTool(toolMetadata: Aff4Container.ToolMetadata): ToolDialect {
    return toolDialects.firstOrNull { it.isApplicable(toolMetadata) }
      ?: defaultToolDialectProvider.get()
  }
}
