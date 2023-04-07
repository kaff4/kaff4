package net.navatwo.kaff4

import com.google.inject.Module
import com.google.inject.util.Modules
import net.navatwo.kaff4.container.Aff4ImageOpenerModule
import net.navatwo.kaff4.model.rdf.Aff4RdfModelPlugin
import net.navatwo.kaff4.rdf.MemoryRdfRepositoryPlugin
import net.navatwo.kaff4.streams.compression.Aff4SnappyPlugin
import net.navatwo.kaff4.streams.compression.deflate.Aff4DeflatePlugin
import net.navatwo.kaff4.streams.compression.lz4.Aff4Lz4Plugin

internal object ReaderModule : Module by Modules.combine(
  RandomsModule,
  MemoryRdfRepositoryPlugin,
  Aff4ImageOpenerModule,
  Aff4CoreModule,
  Aff4BaseStreamModule,
  Aff4LogicalModule,
  Aff4RdfModelPlugin,
  Aff4SnappyPlugin,
  Aff4Lz4Plugin,
  Aff4DeflatePlugin,
)
