package net.navatwo.kaff4.container

import jakarta.inject.Inject
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.rdf.QueryableRdfConnection
import okio.FileSystem
import okio.Path.Companion.toPath
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.RDFWriter
import org.eclipse.rdf4j.rio.Rio

internal class ContainerMetaWriter @Inject constructor() {
  fun write(connection: QueryableRdfConnection, fileSystem: FileSystem, containerArn: Aff4Arn) {
    fileSystem.write("container.description".toPath(), mustCreate = true) {
      writeUtf8(containerArn.stringValue())
      writeUtf8("\n")
    }

    fileSystem.write("version.txt".toPath(), mustCreate = true) {
      writeUtf8("major=1\n")
      writeUtf8("minor=1\n")
      writeUtf8("tool=TEST\n")
    }

    dumpConnectionToTurtle(connection, fileSystem)
  }

  private fun dumpConnectionToTurtle(rdfConnection: QueryableRdfConnection, fileSystem: FileSystem) {
    fileSystem.write("information.turtle".toPath(), mustCreate = true) {
      val writer = Rio.createWriter(RDFFormat.TURTLE, outputStream())
      writer.startRDF()

      dumpNamespacesToWriter(rdfConnection, writer)

      rdfConnection.queryStatements().use { statements ->
        for (statement in statements) {
          writer.handleStatement(statement)
        }
      }

      writer.endRDF()
    }
  }

  private fun dumpNamespacesToWriter(rdfConnection: QueryableRdfConnection, writer: RDFWriter) {
    rdfConnection.queryNamespaces().use { iter ->
      for (ns in iter) {
        writer.handleNamespace(ns.prefix, ns.name)
      }
    }
  }
}
