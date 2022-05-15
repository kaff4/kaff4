package com.github.nava2.aff4.streams

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.ForImages
import com.github.nava2.aff4.ForResources
import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.aff4.meta.rdf.MemoryRdfRepositoryConfiguration
import com.github.nava2.aff4.meta.rdf.RdfRepositoryConfiguration
import com.github.nava2.aff4.meta.rdf.model.ImageStream
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.streams.compression.SnappyModule
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.test.GuiceTestRule
import com.google.inject.Provides
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Singleton

class Aff4BevySourceTest {
  @get:Rule
  val rule: GuiceTestRule = GuiceTestRule(
    Aff4CoreModule,
    Aff4StreamsModule,
    SnappyModule,
    object : KAbstractModule() {
      override fun configure() {
        bind<RdfRepositoryConfiguration>().toInstance(MemoryRdfRepositoryConfiguration)
      }

      @Provides
      @Singleton
      @ForImages
      fun providesFileSystemForImages(@ForResources resourcesFileSystem: FileSystem): FileSystem {
        return resourcesFileSystem.relativeTo("images".toPath())
      }
    }
  )

  @Inject
  @field:ForImages
  private lateinit var imagesFileSystem: FileSystem

  @Inject
  private lateinit var aff4ModelLoader: Aff4Model.Loader

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var bevyFactory: Bevy.Factory

  private lateinit var aff4Model: Aff4Model

  private lateinit var imageStreamConfig: ImageStream
  private lateinit var aff4ImageBevies: Aff4ImageBevies
  private lateinit var bevyChunkCache: BevyChunkCache
  private lateinit var aff4Bevy: Aff4Bevy

  private val chunkSize: Long
    get() = imageStreamConfig.chunkSize.toLong()

  @Before
  fun setup() {
    aff4Model = aff4ModelLoader.load(imagesFileSystem, "Base-Linear.aff4".toPath())
    val imageStreamIri = valueFactory.createIRI("aff4://c215ba20-5648-4209-a793-1f918c723610")
    imageStreamConfig = aff4Model.get(imageStreamIri, ImageStream::class)

    bevyChunkCache = BevyChunkCache(imageStreamConfig.chunkSize)

    aff4ImageBevies = Aff4ImageBevies(
      bevyFactory = bevyFactory,
      fileSystem = aff4Model.imageRootFileSystem,
      imageStreamConfig = imageStreamConfig,
      bevyChunkCache = bevyChunkCache,
    )

    aff4Bevy = aff4ImageBevies.getOrLoadBevy(0)
  }

  @After
  fun after() {
    aff4ImageBevies.close()
    aff4Bevy.close()
  }

  @Test
  fun `open and read bevy source`() {
    createSource().use { bevySource ->
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

    assertThat(aff4Bevy.uncompressedSize).isEqualTo(imageStreamConfig.size)
  }

  @Test
  fun `open and read multiple times has chunks cached`() {
    createSource().use { bevySource ->
      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize)
        assertThat(readSink.size).isEqualTo(chunkSize)
        assertThat(readSink.md5()).isEqualTo("af05fdbda3150e658948ba8b74f1fe82".decodeHex())
      }
    }

    createSource().use { bevySource ->
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
    createSource().use { bevySource ->
      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize * 2)
        assertThat(readSink.size).isEqualTo(chunkSize * 2)
        assertThat(readSink.md5()).isEqualTo("866f93925759a39af236632470789234".decodeHex())
      }
    }
  }

  @Test
  fun `creating sources at location effectively seeks the stream`() {
    createSource(position = chunkSize).use { bevySource ->
      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize)
        assertThat(readSink.size).isEqualTo(chunkSize)
        assertThat(readSink.md5()).isEqualTo("86a8ec10b992e4b9236eb4eadca432d5".decodeHex())
      }
    }

    createSource(position = 0).use { bevySource ->
      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize)
        assertThat(readSink.size).isEqualTo(chunkSize)
        assertThat(readSink.md5()).isEqualTo("af05fdbda3150e658948ba8b74f1fe82".decodeHex())
      }
    }
  }

  @Test
  fun `open and read skip bytes via buffering`() {
    createSource().use { bevySource ->
      bevySource.skip(1024)

      Buffer().use { readSink ->
        bevySource.readFully(readSink, chunkSize)
        assertThat(readSink.size).isEqualTo(chunkSize)
        assertThat(readSink.md5()).isEqualTo("fea53f346a83f6fca5d4fa89ac96e758".decodeHex())
      }
    }
  }

  @Test
  fun `having open sources causes close() to throw`() {
    createSource().use { source ->
      assertThatThrownBy { aff4Bevy.close() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Sources were created and not freed: 1")

      source.close()
      aff4Bevy.close() // no throw
    }
  }

  private fun createSource(position: Long = 0) = aff4Bevy.source(position).buffer()
}
