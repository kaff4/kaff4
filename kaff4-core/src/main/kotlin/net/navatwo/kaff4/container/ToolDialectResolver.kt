package net.navatwo.kaff4.container

import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton
import net.navatwo.kaff4.model.Aff4Container
import net.navatwo.kaff4.model.dialect.DefaultToolDialect
import net.navatwo.kaff4.model.dialect.ToolDialect

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
