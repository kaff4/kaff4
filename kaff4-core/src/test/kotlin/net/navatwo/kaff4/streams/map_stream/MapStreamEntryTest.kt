package net.navatwo.kaff4.streams.map_stream

import com.google.inject.util.Modules
import jakarta.inject.Inject
import net.navatwo.kaff4.Aff4CoreModule
import net.navatwo.kaff4.TestActionScopeModule
import net.navatwo.kaff4.container.Aff4ImageOpenerModule
import net.navatwo.kaff4.io.md5
import net.navatwo.kaff4.isIllegalArgumentException
import net.navatwo.kaff4.rdf.MemoryRdfRepositoryFeatureModule
import net.navatwo.kaff4.satisfies
import net.navatwo.test.GuiceModule
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.Test

class MapStreamEntryTest {
  @GuiceModule
  val module = Modules.combine(
    MemoryRdfRepositoryFeatureModule,
    Aff4CoreModule,
    Aff4ImageOpenerModule,
    TestActionScopeModule,
  )

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Test
  fun `interval characteristics are based off mappedOffset`() {
    val entry = MapStreamEntry(
      mappedOffset = 1024L,
      length = 512L,
      targetOffset = 0,
      targetArn = valueFactory.createIRI("aff4://my-target")
    )

    assertThat(entry.start).isEqualTo(1024L).isEqualTo(entry.mappedOffset)
    assertThat(entry.endExclusive).isEqualTo(1024L + 512L).isEqualTo(entry.mappedEndOffset)
  }

  @Test
  fun `merge successful with overlapping entry`() {
    val entry = MapStreamEntry(
      mappedOffset = 1024L,
      length = 512L,
      targetOffset = 4096,
      targetArn = valueFactory.createIRI("aff4://my-target")
    )

    // Target and Mapped offsets are overlapping: other > this
    assertThat(
      entry.merge(
        entry.copy(mappedOffset = entry.mappedOffset + 256, targetOffset = entry.targetOffset + 256),
      )
    ).isEqualTo(
      entry.copy(mappedOffset = 1024, length = 512 + 256, targetOffset = 4096)
    )

    // Target and Mapped offsets are overlapping: this < other
    assertThat(
      entry.merge(
        entry.copy(mappedOffset = entry.mappedOffset - 256, targetOffset = entry.targetOffset - 256),
      )
    ).isEqualTo(
      entry.copy(mappedOffset = 1024 - 256, length = 512 + 256, targetOffset = 4096 - 256)
    )
  }

  @Test
  fun `can merge successful with aligned entry`() {
    val entry = MapStreamEntry(
      mappedOffset = 1024L,
      length = 512L,
      targetOffset = 4096,
      targetArn = valueFactory.createIRI("aff4://my-target")
    )

    // Target and Mapped offsets are aligned: other > this
    assertThat(
      entry.merge(
        entry.copy(mappedOffset = entry.mappedOffset + entry.length, targetOffset = entry.targetOffset + entry.length),
      )
    ).isEqualTo(
      entry.copy(mappedOffset = 1024, length = 512 + 512, targetOffset = 4096)
    )

    // Target and Mapped offsets are aligned: this < other
    assertThat(
      entry.merge(
        entry.copy(mappedOffset = entry.mappedOffset - entry.length, targetOffset = entry.targetOffset - entry.length),
      )
    ).isEqualTo(
      entry.copy(mappedOffset = 512, length = 512 + 512, targetOffset = 4096 - 512)
    )
  }

  @Test
  fun `can merge successful with contained returning the larger of the two`() {
    val entry = MapStreamEntry(
      mappedOffset = 1024L,
      length = 512L,
      targetOffset = 4096,
      targetArn = valueFactory.createIRI("aff4://my-target")
    )

    // Target and Mapped offsets are aligned: other in this
    assertThat(
      entry.merge(
        entry.copy(mappedOffset = entry.mappedOffset + 128, targetOffset = entry.targetOffset + 128, length = 128),
      )
    ).isEqualTo(entry)

    // Target and Mapped offsets are aligned: this in other
    val containingEntry = entry.copy(
      mappedOffset = entry.mappedOffset - 128,
      targetOffset = entry.targetOffset - 128,
      length = entry.length * 2,
    )
    assertThat(entry.merge(containingEntry)).isEqualTo(containingEntry)
  }

  @Test
  fun `can merge fails when target offsets are misaligned`() {
    val entry = MapStreamEntry(
      mappedOffset = 1024L,
      length = 512L,
      targetOffset = 0,
      targetArn = valueFactory.createIRI("aff4://my-target")
    )

    // Target and Mapped offsets are misaligned
    assertThat(entry).canNotMerge(
      entry.copy(
        mappedOffset = entry.mappedOffset + 256,
        targetOffset = entry.targetOffset,
      )
    )
  }

  @Test
  fun `can merge fails when not aligned or overlapping`() {
    val entry = MapStreamEntry(
      mappedOffset = 1024L,
      length = 512L,
      targetOffset = 0,
      targetArn = valueFactory.createIRI("aff4://my-target")
    )

    // Not overlapping or adjacent mapped offset
    assertThat(entry).canNotMerge(
      entry.copy(
        mappedOffset = entry.mappedOffset + entry.length * 20,
        targetOffset = entry.mappedOffset + entry.length * 20,
      )
    )
  }

  @Test
  fun `can merge fails when target IRIs differ`() {
    val entry = MapStreamEntry(
      mappedOffset = 1024L,
      length = 512L,
      targetOffset = 0,
      targetArn = valueFactory.createIRI("aff4://my-target")
    )

    // different target
    assertThat(entry).canNotMerge(
      entry.copy(
        targetArn = valueFactory.createIRI("aff4://other-target")
      )
    )
  }

  @Test
  fun writeToSink() {
    val entry = MapStreamEntry(
      mappedOffset = 1024L,
      length = 512L,
      targetOffset = 0,
      targetArn = valueFactory.createIRI("aff4://my-target")
    )

    val targetMap = mapOf(entry.targetArn to 4096)

    okio.Buffer().use { buffer ->
      entry.writeToSink(targetMap, buffer)

      // Golden value: Little-endian Long, Long, Long, Int
      // Where final `int` is `4096`, the ID in the target map
      assertThat(buffer).md5(MapStreamEntry.SIZE_BYTES, "fabb72b8055e271fd78eaff656095567")
    }
  }
}

private fun <SELF> SELF.canNotMerge(other: MapStreamEntry): SELF
  where SELF : AbstractObjectAssert<out SELF, MapStreamEntry> {
  return satisfies { entry: MapStreamEntry ->
    assertThat(entry.canMerge(other)).`as` { "!entry.canMerge(other) [entry=$entry, other=$other]" }.isFalse()

    assertThatThrownBy { entry.merge(other) }.isIllegalArgumentException("$entry and $other are not mergable")
  }
}
