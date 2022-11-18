package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.Aff4ImageTestModule
import com.github.nava2.aff4.UnderTest
import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.use
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.streams.compression.Aff4SnappyPlugin
import com.github.nava2.test.GuiceExtension
import com.github.nava2.test.GuiceModule
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.inject.Inject

@ExtendWith(GuiceExtension::class)
class Aff4BevySourceProviderTest {
  @GuiceModule
  val imageTestModule = Aff4ImageTestModule("Base-Linear.aff4", Aff4SnappyPlugin)

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var aff4ImageBeviesFactory: Aff4ImageBevies.Factory

  @Inject
  @field:UnderTest
  private lateinit var aff4Model: Aff4Model

  private lateinit var imageStreamConfig: ImageStream
  private lateinit var aff4ImageBevies: Aff4ImageBevies
  private lateinit var bevyChunkCache: BevyChunkCache
  private lateinit var aff4BevySourceProvider: Aff4BevySourceProvider

  private val bufferedProvider by lazy { aff4BevySourceProvider.buffer() }

  private val chunkSize: Long get() = imageStreamConfig.chunkSize.toLong()

  @BeforeEach
  fun setup() {
    val imageStreamIri = valueFactory.createIRI("aff4://c215ba20-5648-4209-a793-1f918c723610")
    imageStreamConfig = aff4Model.get(imageStreamIri, ImageStream::class)

    bevyChunkCache = BevyChunkCache(imageStreamConfig.chunkSize)

    aff4ImageBevies = aff4ImageBeviesFactory.create(imageStreamConfig, bevyChunkCache)

    aff4BevySourceProvider = aff4ImageBevies.getOrLoadBevy(0)
  }

  @Test
  fun `open and read bevy source`() {
    bufferedProvider.use { bevySource ->
      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize)
        assertThat(readSink.size).isEqualTo(chunkSize)
        assertThat(readSink.md5()).isEqualTo("af05fdbda3150e658948ba8b74f1fe82".decodeHex())
      }

      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize)
        assertThat(readSink.size).isEqualTo(chunkSize)
        assertThat(readSink.md5()).isEqualTo("86a8ec10b992e4b9236eb4eadca432d5".decodeHex())
      }
    }

    assertThat(aff4BevySourceProvider.uncompressedSize).isEqualTo(imageStreamConfig.size)
  }

  @Test
  fun `open and read multiple times has chunks cached`() {
    bufferedProvider.use { bevySource ->
      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize)
        assertThat(readSink.size).isEqualTo(chunkSize)
        assertThat(readSink.md5()).isEqualTo("af05fdbda3150e658948ba8b74f1fe82".decodeHex())
      }
    }

    bufferedProvider.use { bevySource ->
      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize)
        assertThat(readSink.size).isEqualTo(chunkSize)
        assertThat(readSink.md5()).isEqualTo("af05fdbda3150e658948ba8b74f1fe82".decodeHex())
      }
    }

    assertThat(bevyChunkCache.stats().hitRate()).isEqualTo(0.5)
  }

  @Test
  fun `open and read gt chunk size`() {
    bufferedProvider.use { bevySource ->
      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize * 2)
        assertThat(readSink.size).isEqualTo(chunkSize * 2)
        assertThat(readSink.md5()).isEqualTo("866f93925759a39af236632470789234".decodeHex())
      }
    }
  }

  @Test
  fun `creating sources at location effectively seeks the stream`() {
    bufferedProvider.use(position = chunkSize) { bevySource ->
      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize)
        assertThat(readSink.size).isEqualTo(chunkSize)
        assertThat(readSink.md5()).isEqualTo("86a8ec10b992e4b9236eb4eadca432d5".decodeHex())
      }
    }

    bufferedProvider.use(position = 0) { bevySource ->
      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize)
        assertThat(readSink.size).isEqualTo(chunkSize)
        assertThat(readSink.md5()).isEqualTo("af05fdbda3150e658948ba8b74f1fe82".decodeHex())
      }
    }
  }

  @Test
  fun `open and read skip bytes via buffering`() {
    bufferedProvider.use { bevySource ->
      bevySource.skip(1024)

      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize)
        assertThat(readSink.size).isEqualTo(chunkSize)
        assertThat(readSink.md5()).isEqualTo("fea53f346a83f6fca5d4fa89ac96e758".decodeHex())
      }
    }
  }
}
