package com.github.nava2.aff4.dialects

import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.dialect.DefaultToolDialect
import com.github.nava2.aff4.model.dialect.DialectTypeResolver
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.guice.KAbstractModule
import com.google.inject.Provides
import javax.inject.Singleton

class Pyaff4Version11ToolDialect private constructor(override val typeResolver: DialectTypeResolver) : ToolDialect {
  override fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean {
    return toolMetadata == Aff4Container.ToolMetadata(version = "1.1", tool = "pyaff4")
  }

  object Module : KAbstractModule() {
    override fun configure() {
      bindSet<ToolDialect> {
        to<Pyaff4Version11ToolDialect>()
      }
    }

    @Provides
    @Singleton
    internal fun providesDreamToolDialect(
      @DefaultToolDialect defaultToolDialect: ToolDialect,
    ): Pyaff4Version11ToolDialect {
      val dreamDialectTypeResolver = DialectTypeResolver.Builder.withExisting(defaultToolDialect.typeResolver)
        .apply {
          remove(ZipSegment::class)
          register(ZipSegment::class, "aff4:zip_segment")
        }
        .build()
      return Pyaff4Version11ToolDialect(dreamDialectTypeResolver)
    }
  }
}
