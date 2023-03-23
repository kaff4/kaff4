package com.github.nava2.aff4.streams

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.TestActionScopeModule
import com.github.nava2.aff4.TestRandomsModule
import com.github.nava2.aff4.container.Aff4ContainerBuilderModule
import com.github.nava2.aff4.container.Aff4ImageOpenerModule
import com.github.nava2.aff4.model.Aff4StreamOpenerModule
import com.github.nava2.aff4.model.rdf.Aff4RdfModelPlugin
import com.github.nava2.guice.KAbstractModule

object TestAff4ContainerBuilderModule : KAbstractModule() {
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
