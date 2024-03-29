package net.navatwo.kaff4.streams.image_stream

import jakarta.inject.Inject
import net.navatwo.kaff4.Aff4ImageTestModule
import net.navatwo.kaff4.BaseLinear
import net.navatwo.kaff4.UnderTest
import net.navatwo.kaff4.io.buffer
import net.navatwo.kaff4.io.md5
import net.navatwo.kaff4.io.readAll
import net.navatwo.kaff4.io.use
import net.navatwo.kaff4.model.Aff4Model
import net.navatwo.kaff4.model.VerifiableStreamProvider
import net.navatwo.kaff4.model.rdf.Hash
import net.navatwo.kaff4.model.rdf.ImageStream
import net.navatwo.test.GuiceModule
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Aff4ImageStreamSourceProviderTest {
  @GuiceModule
  val module = Aff4ImageTestModule.BaseLinear

  @Inject
  private lateinit var aff4ImageStreamSourceProviderLoader: Aff4ImageStreamSourceProvider.Loader

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  @field:UnderTest
  private lateinit var aff4Model: Aff4Model

  private lateinit var imageStreamConfig: ImageStream

  private lateinit var aff4ImageStreamSourceProvider: Aff4ImageStreamSourceProvider
  private val bufferedProvider by lazy { aff4ImageStreamSourceProvider.buffer() }

  private val chunkSize: Long
    get() = imageStreamConfig.chunkSize.toLong()

  @BeforeEach
  fun setup() {
    val imageStreamIri = valueFactory.createIRI("aff4://c215ba20-5648-4209-a793-1f918c723610")
    imageStreamConfig = aff4Model.get(imageStreamIri, ImageStream::class)

    aff4ImageStreamSourceProvider = aff4ImageStreamSourceProviderLoader.load(imageStreamConfig)
  }

  @Test
  fun `open and chunks`() {
    bufferedProvider.use { imageStreamSource ->
      assertThat(imageStreamSource).md5(chunkSize, "af05fdbda3150e658948ba8b74f1fe82")
      assertThat(imageStreamSource).md5(chunkSize, "86a8ec10b992e4b9236eb4eadca432d5")
    }
  }

  @Test
  fun `open and read multiple times has same read`() {
    bufferedProvider.use { imageStreamSource ->
      assertThat(imageStreamSource).md5(chunkSize, "af05fdbda3150e658948ba8b74f1fe82")
    }

    bufferedProvider.use { imageStreamSource ->
      assertThat(imageStreamSource).md5(chunkSize, "af05fdbda3150e658948ba8b74f1fe82")
    }
  }

  @Test
  fun `open and read gt chunk size`() {
    bufferedProvider.use { imageStreamSource ->
      assertThat(imageStreamSource).md5(chunkSize * 2, "866f93925759a39af236632470789234")
    }
  }

  @Test
  fun `creating sources at location effectively seeks the stream`() {
    bufferedProvider.use(position = chunkSize) { imageStreamSource ->
      assertThat(imageStreamSource).md5(chunkSize, "86a8ec10b992e4b9236eb4eadca432d5")
    }

    bufferedProvider.use(position = 0) { imageStreamSource ->
      assertThat(imageStreamSource).md5(chunkSize, "af05fdbda3150e658948ba8b74f1fe82")
    }
  }

  @Test
  fun `open and read skip bytes via buffering`() {
    bufferedProvider.use { imageStreamSource ->
      imageStreamSource.skipFully(1024)

      assertThat(imageStreamSource).md5(chunkSize, "fea53f346a83f6fca5d4fa89ac96e758")
    }
  }

  @Test
  fun `reading past end truncates`() {
    bufferedProvider.use(position = imageStreamConfig.size - 100) { imageStreamSource ->
      Buffer().use { readSink ->
        assertThat(imageStreamSource.readAll(readSink)).isEqualTo(100)
        assertThat(readSink.size).isEqualTo(100)
        assertThat(readSink.md5()).isEqualTo("6d0bb00954ceb7fbee436bb55a8397a9".decodeHex())
      }
    }
  }

  @Test
  fun `hashes match`() {
    bufferedProvider.use { imageStreamSource ->
      Buffer().use { readSink ->
        assertThat(imageStreamSource.readAll(readSink)).isEqualTo(imageStreamConfig.size)
        assertThat(readSink.md5()).isEqualTo(imageStreamConfig.linearHashes.single { it is Hash.Md5 }.value)
        assertThat(readSink.sha1()).isEqualTo(imageStreamConfig.linearHashes.single { it is Hash.Sha1 }.value)
      }
    }

    assertThat(aff4ImageStreamSourceProvider.verify(aff4Model)).isEqualTo(VerifiableStreamProvider.Result.Success)
  }
}
