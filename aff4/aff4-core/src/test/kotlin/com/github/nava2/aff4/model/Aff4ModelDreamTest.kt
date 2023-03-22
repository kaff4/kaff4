package com.github.nava2.aff4.model

import com.github.nava2.aff4.Aff4ImageTestModule
import com.github.nava2.aff4.Dream
import com.github.nava2.aff4.UnderTest
import com.github.nava2.aff4.io.decode
import com.github.nava2.aff4.model.rdf.FileImage
import com.github.nava2.aff4.model.rdf.Hash
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.aff4.model.rdf.createArn
import com.github.nava2.aff4.parseZonedDateTime
import com.github.nava2.aff4.satisfies
import com.github.nava2.test.GuiceModule
import okio.FileSystem
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.Test
import javax.inject.Inject

class Aff4ModelDreamTest {
  @GuiceModule
  val module = Aff4ImageTestModule.Dream

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  @field:UnderTest
  private lateinit var aff4Image: Aff4Image

  @Inject
  @field:UnderTest
  private lateinit var aff4Model: Aff4Model

  private val imageFileSystem: FileSystem by lazy { aff4Model.containerContext.containers.first().dataFileSystem }

  @Test
  fun `model loads correctly`() {
    assertThat(aff4Image.containers).singleElement().satisfies { container ->
      assertThat(container.metadata).isEqualTo(Aff4Container.ToolMetadata("1.1", "pyaff4"))
      assertThat(container.containerArn).isEqualTo(arn("aff4://5aea2dd0-32b4-4c61-a9db-677654be6f83"))
    }
  }

  @Test
  fun `loads files`() {
    assertThat(aff4Model.query<FileImage>().toSet()).singleElement().isEqualTo(
      FileImage(
        arn = arn("aff4://5aea2dd0-32b4-4c61-a9db-677654be6f83//test_images/AFF4-L/dream.txt"),
        originalFileName = "./test_images/AFF4-L/dream.txt".toPath(),
        size = 8688,
        birthTime = parseZonedDateTime("2018-09-17T13:42:20+10:00"),
        lastAccessed = parseZonedDateTime("2018-10-23T11:08:19+10:00"),
        lastWritten = parseZonedDateTime("2018-09-17T13:42:20+10:00"),
        recordChanged = parseZonedDateTime("2018-09-17T13:42:20+10:00"),
      ),
    )

    val zipSegment = aff4Model.query<ZipSegment>().single()
    assertThat(zipSegment).isEqualTo(
      ZipSegment(
        arn = arn("aff4://5aea2dd0-32b4-4c61-a9db-677654be6f83//test_images/AFF4-L/dream.txt"),
        size = 8688,
        linearHashes = setOf(
          Hash.Md5.decode("75d83773f8d431a3ca91bfb8859e486d"),
          Hash.Sha1.decode("9ae1b46bead70c322eef7ac8bc36a8ea2055595c"),
        ),
        stored = arn("aff4://5aea2dd0-32b4-4c61-a9db-677654be6f83"),
      ),
    )

    assertThat(imageFileSystem.exists(zipSegment.segmentPath)).isTrue()
  }

  private fun arn(iri: String) = valueFactory.createArn(iri)
}
