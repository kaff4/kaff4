package com.github.nava2.aff4.container

import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.dialect.DefaultToolDialect
import com.github.nava2.aff4.model.dialect.ToolDialect
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
internal class ToolDialectResolver @Inject constructor(
  private val toolDialectsProvider: Provider<Set<ToolDialect>>,
  @DefaultToolDialect private val defaultToolDialectProvider: Provider<ToolDialect>,
) {
  fun forTool(toolMetadata: Aff4Container.ToolMetadata): ToolDialect {
    val applicableDialect = toolDialectsProvider.get().firstOrNull { it.isApplicable(toolMetadata) }
    return applicableDialect ?: defaultToolDialectProvider.get()
  }
}
