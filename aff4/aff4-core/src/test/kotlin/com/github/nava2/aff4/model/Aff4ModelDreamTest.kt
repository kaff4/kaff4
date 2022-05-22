package com.github.nava2.aff4.model

import com.github.nava2.aff4.Aff4ImageTestRule
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.rdf.FileImage
import com.github.nava2.aff4.model.rdf.Hash
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.test.GuiceTestRule
import okio.ByteString.Companion.decodeHex
import okio.FileSystem
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

class Aff4ModelDreamTest {
  @get:Rule
  val rule: GuiceTestRule = Aff4ImageTestRule("dream.aff4")

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var aff4Model: Aff4Model

  @Inject
  @field:ForImageRoot
  private lateinit var imageFileSystem: FileSystem

  @Test
  fun `model loads correctly`() {
    assertThat(aff4Model.metadata).isEqualTo(Aff4Model.Metadata("1.1", "pyaff4"))
    assertThat(aff4Model.containerArn).isEqualTo(arn("aff4://5aea2dd0-32b4-4c61-a9db-677654be6f83"))
  }

  @Test
  fun `loads files`() {
    assertThat(aff4Model.query(FileImage::class).single()).isEqualTo(
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

    val zipSegment = aff4Model.query(ZipSegment::class).single()
    assertThat(zipSegment).isEqualTo(
      ZipSegment(
        arn = arn("aff4://5aea2dd0-32b4-4c61-a9db-677654be6f83//test_images/AFF4-L/dream.txt"),
        size = 8688,
        linearHashes = listOf(
          Hash.Md5("75d83773f8d431a3ca91bfb8859e486d".decodeHex()),
          Hash.Sha1("9ae1b46bead70c322eef7ac8bc36a8ea2055595c".decodeHex()),
        ),
        stored = arn("aff4://5aea2dd0-32b4-4c61-a9db-677654be6f83"),
      ),
    )

    assertThat(imageFileSystem.exists(zipSegment.segmentPath)).isTrue()
  }

  private fun arn(iri: String) = valueFactory.createIRI(iri)
}
