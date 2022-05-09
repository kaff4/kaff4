package com.github.nava2.aff4.meta.parser

import com.github.nava2.aff4.io.SourceProvider
import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.use
import com.github.nava2.aff4.meta.Aff4Model
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.getInstance
import com.github.nava2.logging.Logging
import com.google.inject.Injector
import com.google.inject.Provides
import com.google.inject.assistedinject.FactoryModuleBuilder
import okio.Path
import okio.Source
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton

val logger = Logging.getLogger()

@Singleton
class RdfProvider @Inject constructor(
  private val injector: Injector,
) {
  fun parseStream(
    imagePath: Path,
    sourceProvider: SourceProvider<Source>,
    consumer: Consumer<Aff4Model>,
  ) {
    val rdfParser = Rio.createParser(RDFFormat.TURTLE)


    val privateInjector = injector.createChildInjector(
      ScopedParserModule(imagePath),
      object : KAbstractModule() {
        override fun configure() {
          bind<NamespacesContainer>().toInstance(NamespacesContainer())

          install(
            FactoryModuleBuilder()
              .implement(RdfModelParsingHandler.ParsingCallbacks::class.java, Aff4ModelParsingCallbacks::class.java)
              .build(Aff4ModelParsingCallbacks.Factory::class.java)
          )
        }

        @Provides
        fun providesNamespaces(namespacesContainer: NamespacesContainer) = namespacesContainer.namespaces
      }
    )

    val callbacksFactory = privateInjector.getInstance<Aff4ModelParsingCallbacks.Factory>()

    rdfParser.setRDFHandler(RdfModelParsingHandler(callbacksFactory.create(consumer)))

    sourceProvider.buffer().use { source ->
      rdfParser.parse(source.inputStream())
    }
  }
}

