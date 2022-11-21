package com.github.nava2.aff4.rdf

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.isIllegalStateException
import com.github.nava2.test.GuiceExtension
import com.github.nava2.test.GuiceModule
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.ObjectAssert
import org.eclipse.rdf4j.model.Statement
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.inject.Inject

@ExtendWith(GuiceExtension::class)
internal class RealRdfExecutorTest {
  @GuiceModule
  val modules = listOf(Aff4CoreModule, MemoryRdfRepositoryPlugin)

  @Inject
  lateinit var realRdfExecutor: RealRdfExecutor

  private lateinit var statements: List<Statement>

  @BeforeEach
  fun setup() {
    statements = realRdfExecutor.withReadWriteSession { connection ->
      val vf = connection.valueFactory
      connection.add(
        vf.createIRI("test://bar"),
        vf.createIRI("test://baz") to vf.createLiteral(0),
        vf.createIRI("test://buzz") to vf.createLiteral(2),
      )

      connection.queryStatements().use { it.toList() }
    }
  }

  @Test
  fun `promoting read-only to read-write throws`() {
    realRdfExecutor.withReadOnlySession {
      assertThatThrownBy {
        realRdfExecutor.withReadWriteSession {
          fail("This should never execute")
        }
      }.isIllegalStateException("Can not promote read-only connection to read-write.")
    }

    assertThat(realRdfExecutor).canQueryStatements(statements)
  }

  @Test
  fun `nesting read-only connection uses same connection each time`() {
    realRdfExecutor.withReadOnlySession { connection1 ->
      realRdfExecutor.withReadOnlySession { connection2 ->
        assertThat(connection2).isEqualTo(connection1)

        realRdfExecutor.withReadOnlySession { connection3 ->
          assertThat(connection3).isEqualTo(connection1)
        }
      }
    }

    assertThat(realRdfExecutor).canQueryStatements(statements)
  }

  @Test
  fun `nesting read-only in read-write connection successfully wraps into read-only`() {
    lateinit var fooStatement: Statement

    realRdfExecutor.withReadWriteSession { readWriteConnection ->
      val vf = readWriteConnection.valueFactory
      val fooSubject = vf.createIRI("test://foo")
      fooStatement = vf.createStatement(
        fooSubject,
        vf.createIRI("test://bar"),
        vf.createLiteral("other"),
      )
      readWriteConnection.add(listOf(fooStatement))

      realRdfExecutor.withReadOnlySession { readOnlyConnection1 ->
        assertThat(readOnlyConnection1).isInstanceOf(RealRdfExecutor.NestedRdfConnection::class.java)
        readOnlyConnection1 as RealRdfExecutor.NestedRdfConnection
        assertThat(readOnlyConnection1.queryableRdfConnection).isEqualTo(readWriteConnection)

        realRdfExecutor.withReadOnlySession { readOnlyConnection2 ->
          // inner nesting doesn't need a wrapper, its the same value
          assertThat(readOnlyConnection2).isEqualTo(readOnlyConnection1)

          assertThat(
            readOnlyConnection2.queryStatements(subj = fooSubject)
              .use { it.toList() },
          ).singleElement()
            .isEqualTo(fooStatement)
        }
      }
    }

    assertThat(realRdfExecutor).canQueryStatements(statements + fooStatement)
  }
}

private fun ObjectAssert<out RdfExecutor>.canQueryStatements(statements: Collection<Statement>) {
  extracting { rdfConnectionPool ->
    rdfConnectionPool.withReadOnlySession { connection ->
      connection.queryStatements().use { it.toList() }
    }
  }
    .asList()
    .containsExactlyInAnyOrderElementsOf(statements)
}
