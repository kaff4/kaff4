package com.github.nava2.aff4.model.dialect

import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.guice.KAff4AbstractModule
import com.google.inject.Provides
import javax.inject.Singleton

class Pyaff4Version11ToolDialect private constructor(override val typeResolver: DialectTypeResolver) : ToolDialect {
  override fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean {
    return toolMetadata == Aff4Container.ToolMetadata(version = "1.1", tool = "pyaff4")
  }

  object Module : KAff4AbstractModule() {
    override fun configure() {
      install(Aff4ToolDialectModule)

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
          register(ZipSegment::class, "aff4:zip_segment", "aff4:ZipSegment")
        }
        .build()
      return Pyaff4Version11ToolDialect(dreamDialectTypeResolver)
    }
  }
}
