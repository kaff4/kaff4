package com.github.nava2.aff4.container

import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.rdf.RdfConnectionScoped
import com.github.nava2.aff4.rdf.ScopedConnection
import okio.FileSystem
import okio.Path.Companion.toPath
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.RDFWriter
import org.eclipse.rdf4j.rio.Rio
import javax.inject.Inject

@RdfConnectionScoped
internal class ContainerMetaWriter @Inject constructor(
  private val scopedConnection: ScopedConnection,
) {
  fun write(fileSystem: FileSystem, containerArn: Aff4Arn) {
    fileSystem.write("container.description".toPath(), mustCreate = true) {
      writeUtf8(containerArn.stringValue())
      writeUtf8("\n")
    }

    fileSystem.write("version.txt".toPath(), mustCreate = true) {
      writeUtf8("major=1\n")
      writeUtf8("minor=1\n")
      writeUtf8("tool=TEST\n")
    }

    dumpConnectionToTurtle(fileSystem)
  }

  private fun dumpConnectionToTurtle(fileSystem: FileSystem) {
    fileSystem.write("information.turtle".toPath(), mustCreate = true) {
      val writer = Rio.createWriter(RDFFormat.TURTLE, outputStream())
      writer.startRDF()

      dumpNamespacesToWriter(writer)

      scopedConnection.queryStatements().use { statements ->
        for (statement in statements) {
          writer.handleStatement(statement)
        }
      }

      writer.endRDF()
    }
  }

  private fun dumpNamespacesToWriter(writer: RDFWriter) {
    scopedConnection.queryNamespaces().use { iter ->
      for (ns in iter) {
        writer.handleNamespace(ns.prefix, ns.name)
      }
    }
  }
}
