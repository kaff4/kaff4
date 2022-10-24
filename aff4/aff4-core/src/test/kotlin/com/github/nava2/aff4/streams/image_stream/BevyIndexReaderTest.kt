package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.Aff4ImageTestRule
import com.github.nava2.aff4.UnderTest
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.streams.compression.Aff4SnappyModule
import okio.Timeout
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

class BevyIndexReaderTest {
  @get:Rule
  val rule = Aff4ImageTestRule("Base-Linear.aff4", Aff4SnappyModule)

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var bevyOpener: Bevy.Opener

  @Inject
  private lateinit var bevyIndexReaderFactory: BevyIndexReader.Factory

  @Inject
  @field:UnderTest
  private lateinit var aff4Model: Aff4Model

  private lateinit var imageStreamIri: IRI
  private lateinit var imageStreamConfig: ImageStream

  private val chunkSize: Long
    get() = imageStreamConfig.chunkSize.toLong()

  @Before
  fun setup() {
    imageStreamIri = valueFactory.createIRI("aff4://c215ba20-5648-4209-a793-1f918c723610")
    imageStreamConfig = aff4Model.get(imageStreamIri, ImageStream::class)
  }

  @Test
  fun `open and read index values`() {
    val bevy = bevyOpener.open(imageStreamConfig, 0)
    bevyIndexReaderFactory.create(imageStreamConfig, bevy).use { bevyIndexReader ->
      val firstIndex = IndexValue(0L, 1974)

      assertThat(bevyIndexReader.readIndexContaining(0, Timeout.NONE)).isEqualTo(firstIndex)
      assertThat(bevyIndexReader.readIndexContaining(1024, Timeout.NONE)).isEqualTo(firstIndex)
      assertThat(bevyIndexReader.readIndexContaining(chunkSize - 1, Timeout.NONE)).isEqualTo(firstIndex)

      val secondIndex = IndexValue(1974L, 24_668)
      assertThat(bevyIndexReader.readIndexContaining(chunkSize, Timeout.NONE)).isEqualTo(secondIndex)
      assertThat(bevyIndexReader.readIndexContaining(chunkSize + 128, Timeout.NONE)).isEqualTo(secondIndex)
      assertThat(bevyIndexReader.readIndexContaining(chunkSize * 2 - 1, Timeout.NONE)).isEqualTo(secondIndex)

      assertThat(bevyIndexReader.readIndexContaining(70 * chunkSize, Timeout.NONE))
        .isEqualTo(IndexValue(2_126_894L, 24_321))

      assertThat(bevyIndexReader.readIndexContaining(0, Timeout.NONE)).isEqualTo(firstIndex)
      assertThat(bevyIndexReader.readIndexContaining(chunkSize + 128, Timeout.NONE)).isEqualTo(secondIndex)
    }
  }

  @Test
  fun `random read index locations`() {
    val bevy = bevyOpener.open(imageStreamConfig, 0)
    bevyIndexReaderFactory.create(imageStreamConfig, bevy).use { bevyIndexReader ->
      val firstIndex = IndexValue(0L, 1974)

      assertThat(bevyIndexReader.readIndexContaining(0, Timeout.NONE)).isEqualTo(firstIndex)

      assertThat(bevyIndexReader.readIndexContaining(70 * chunkSize, Timeout.NONE))
        .isEqualTo(IndexValue(2_126_894L, 24_321))

      // back track read, hits cache
      assertThat(bevyIndexReader.readIndexContaining(0, Timeout.NONE)).isEqualTo(firstIndex)

      // cache miss, causes us to need a new stream
      val secondIndex = IndexValue(1974L, 24_668)
      assertThat(bevyIndexReader.readIndexContaining(chunkSize, Timeout.NONE)).isEqualTo(secondIndex)
    }
  }

  @Test
  fun `invalid positions throw`() {
    val bevy = bevyOpener.open(imageStreamConfig, 0)
    bevyIndexReaderFactory.create(imageStreamConfig, bevy).use { bevyIndexReader ->
      assertThatThrownBy { bevyIndexReader.readIndexContaining(-1, Timeout.NONE) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("bevyPosition must be positive")
    }
  }
}
