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
import okio.FileSystem
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Singleton

class BevyIndexReaderTest {
  @get:Rule
  val rule: GuiceTestRule = GuiceTestRule(
    Aff4CoreModule,
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

  private lateinit var imageStreamIri: IRI
  private lateinit var imageStreamConfig: ImageStream

  private val chunkSize: Long
    get() = imageStreamConfig.chunkSize.toLong()

  @Before
  fun setup() {
    aff4Model = aff4ModelLoader.load(imagesFileSystem, "Base-Linear.aff4".toPath())
    imageStreamIri = valueFactory.createIRI("aff4://c215ba20-5648-4209-a793-1f918c723610")
    imageStreamConfig = aff4Model.get(imageStreamIri, ImageStream::class)
  }

  @Test
  fun `open and read index values`() {
    val bevy = bevyFactory.create(imageStreamIri, 0)
    BevyIndexReader(aff4Model.imageRootFileSystem, imageStreamConfig, bevy).use { bevyIndexReader ->
      val firstIndex = IndexValue(0L, 1974)

      assertThat(bevyIndexReader.readIndexContaining(0)).isEqualTo(firstIndex)
      assertThat(bevyIndexReader.readIndexContaining(1024)).isEqualTo(firstIndex)
      assertThat(bevyIndexReader.readIndexContaining(chunkSize - 1)).isEqualTo(firstIndex)

      val secondIndex = IndexValue(1974L, 24_668)
      assertThat(bevyIndexReader.readIndexContaining(chunkSize)).isEqualTo(secondIndex)
      assertThat(bevyIndexReader.readIndexContaining(chunkSize + 128)).isEqualTo(secondIndex)
      assertThat(bevyIndexReader.readIndexContaining(chunkSize * 2 - 1)).isEqualTo(secondIndex)

      assertThat(bevyIndexReader.readIndexContaining(70 * chunkSize))
        .isEqualTo(IndexValue(2_126_894L, 24_321))
    }
  }

  @Test
  fun `random read index locations`() {
    val bevy = bevyFactory.create(imageStreamIri, 0)
    BevyIndexReader(aff4Model.imageRootFileSystem, imageStreamConfig, bevy).use { bevyIndexReader ->
      val firstIndex = IndexValue(0L, 1974)

      assertThat(bevyIndexReader.readIndexContaining(0)).isEqualTo(firstIndex)

      assertThat(bevyIndexReader.readIndexContaining(70 * chunkSize))
        .isEqualTo(IndexValue(2_126_894L, 24_321))

      // back track read, hits cache
      assertThat(bevyIndexReader.readIndexContaining(0)).isEqualTo(firstIndex)

      // cache miss, causes us to need a new stream
      val secondIndex = IndexValue(1974L, 24_668)
      assertThat(bevyIndexReader.readIndexContaining(chunkSize)).isEqualTo(secondIndex)
    }
  }

  @Test
  fun `invalid positions throw`() {
    val bevy = bevyFactory.create(imageStreamIri, 0)
    BevyIndexReader(aff4Model.imageRootFileSystem, imageStreamConfig, bevy).use { bevyIndexReader ->
      assertThatThrownBy { bevyIndexReader.readIndexContaining(-1) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("bevyPosition must be positive")
    }
  }
}
