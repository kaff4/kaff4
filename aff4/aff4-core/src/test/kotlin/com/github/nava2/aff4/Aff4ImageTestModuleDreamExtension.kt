package com.github.nava2.aff4

import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.dialect.DefaultToolDialect
import com.github.nava2.aff4.model.dialect.DialectTypeResolver
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.aff4.streams.compression.Aff4SnappyPlugin
import com.google.inject.Module
import com.google.inject.Provides
import javax.inject.Singleton

val Aff4ImageTestModule.Companion.Dream: Module
  get() = object : Aff4ImageTestModule(imageName = "dream.aff4") {
    override fun configureOther() {
      install(Aff4SnappyPlugin)
      install(Aff4LogicalModule)

      bindSet<ToolDialect> {
        to<DreamToolDialect>()
      }
    }

    @Provides
    @Singleton
    internal fun providesDreamToolDialect(
      @DefaultToolDialect defaultToolDialect: ToolDialect,
    ): DreamToolDialect {
      val dreamDialectTypeResolver = DialectTypeResolver.Builder.withExisting(defaultToolDialect.typeResolver)
        .apply {
          remove(ZipSegment::class)
          register(ZipSegment::class, "aff4:zip_segment")
        }
        .build()
      return DreamToolDialect(dreamDialectTypeResolver)
    }
  }

private class DreamToolDialect(override val typeResolver: DialectTypeResolver) : ToolDialect {
  override fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean {
    return toolMetadata == Aff4Container.ToolMetadata(version = "1.1", tool = "pyaff4")
  }
}
