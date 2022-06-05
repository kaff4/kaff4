package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.interval_tree.IntervalTree
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryModule
import com.github.nava2.aff4.rdf.RdfRepositoryModule
import com.github.nava2.test.GuiceTestRule
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

class MapStreamEntryIntervalTreeExtensionsTest {
  @get:Rule
  val rule = GuiceTestRule(RdfRepositoryModule, MemoryRdfRepositoryModule)

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Test
  fun `empty tree compression returns self`() {
    val emptyTree = IntervalTree<MapStreamEntry>()
    assertThat(emptyTree.compressedSequence().toList()).isEmpty()
  }

  @Test
  fun `single element compression return self`() {
    val entry = MapStreamEntry(
      mappedOffset = 0L,
      length = 1L,
      targetOffset = 1024L,
      targetArn = valueFactory.createIRI("aff4://target-0"),
    )
    val tree = IntervalTree(entry)

    assertThat(tree.compressedSequence().toList()).singleElement().isEqualTo(entry)
  }

  @Test
  fun `similar elements which can merge are merged`() {
    val entry0 = MapStreamEntry(
      mappedOffset = 0L,
      length = 1L,
      targetOffset = 1024L,
      targetArn = valueFactory.createIRI("aff4://target-0"),
    )
    val entry1 = entry0.copy(mappedOffset = 1L, targetOffset = 1025L)

    val entry2 = entry1.copy(mappedOffset = 2L, targetOffset = 1024L)
    val entry3 = entry1.copy(mappedOffset = 3L, targetOffset = 1025L)

    val entry4 = entry1.copy(mappedOffset = 5L, targetOffset = 1027L)

    val entry5 = entry1.copy(
      mappedOffset = 6L,
      targetOffset = 1028L,
      targetArn = valueFactory.createIRI("aff4://target-1"),
    )

    val tree = IntervalTree(entry0, entry1, entry2, entry3, entry4, entry5)

    assertThat(tree.compressedSequence().toList()).containsExactly(
      entry0.copy(length = 2L),
      entry2.copy(length = 2L),
      entry4,
      entry5,
    )
  }
}
