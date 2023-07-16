package net.navatwo.kaff4

import com.google.inject.Module
import com.google.inject.util.Modules
import net.navatwo.kaff4.container.Aff4ImageOpenerModule
import net.navatwo.kaff4.model.rdf.Aff4RdfModelFeatureModule
import net.navatwo.kaff4.rdf.MemoryRdfRepositoryFeatureModule
import net.navatwo.kaff4.streams.compression.Aff4SnappyFeatureModule
import net.navatwo.kaff4.streams.compression.deflate.Aff4DeflateFeatureModule
import net.navatwo.kaff4.streams.compression.lz4.Aff4Lz4FeatureModule

internal object ReaderModule : Module by Modules.combine(
  RandomsModule,
  MemoryRdfRepositoryFeatureModule,
  Aff4ImageOpenerModule,
  Aff4CoreModule,
  Aff4BaseStreamModule,
  Aff4LogicalModule,
  Aff4RdfModelFeatureModule,
  Aff4SnappyFeatureModule,
  Aff4Lz4FeatureModule,
  Aff4DeflateFeatureModule,
)
