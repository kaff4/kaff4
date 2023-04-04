package net.navatwo.kaff4.streams

import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.kaff4.Aff4CoreModule
import net.navatwo.kaff4.TestActionScopeModule
import net.navatwo.kaff4.TestRandomsModule
import net.navatwo.kaff4.container.Aff4ContainerBuilderModule
import net.navatwo.kaff4.container.Aff4ImageOpenerModule
import net.navatwo.kaff4.model.Aff4StreamOpenerModule
import net.navatwo.kaff4.model.rdf.Aff4RdfModelPlugin

object TestAff4ContainerBuilderModule : KAff4AbstractModule() {
  override fun configure() {
    install(TestRandomsModule)
    install(TestActionScopeModule)

    install(Aff4ContainerBuilderModule)
    install(Aff4CoreModule)

    install(Aff4StreamOpenerModule)
    install(Aff4ImageOpenerModule)
    install(Aff4RdfModelPlugin)
  }
}
