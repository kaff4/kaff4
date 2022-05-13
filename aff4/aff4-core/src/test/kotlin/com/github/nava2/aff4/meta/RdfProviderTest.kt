package com.github.nava2.aff4.meta

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.ForImages
import com.github.nava2.aff4.ForResources
import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.aff4.io.sourceProvider
import com.github.nava2.aff4.meta.parser.RdfProvider
import com.github.nava2.aff4.meta.parser.TestRdfRepositoryModule
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.test.GuiceTestRule
import com.google.inject.Provides
import okio.ByteString.Companion.decodeHex
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.openZip
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

class RdfProviderTest {
  @get:Rule
  val rule: GuiceTestRule = GuiceTestRule(
    Aff4CoreModule,
    TestRdfRepositoryModule,
    object : KAbstractModule() {
      override fun configure() = Unit

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
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var rdfProvider: RdfProvider

  private lateinit var aff4FileSystem: FileSystem
  private val turtlePath = "information.turtle".toPath()

  @Before
  fun setup() {
    aff4FileSystem = imagesFileSystem.openZip("Base-Linear.aff4".toPath())
  }

  @Test
  @Suppress("LongMethod")
  fun `parse turtle rdf from zip content`() {
    val consumer = CollectingModelConsumer()
    rdfProvider.parseStream(turtlePath, aff4FileSystem.sourceProvider(turtlePath), consumer)

    assertThat(consumer.models)
      .containsExactlyInAnyOrder(
        BlockHashes(
          arn = arn("aff4://c215ba20-5648-4209-a793-1f918c723610/", "blockhash.md5"),
          hash = Hash.Sha512(
            (
              "9062f1c9f48438a6875a60b7e1323151e8ff583c8531ca7806d6c29b7d961ceddba8783e8e4c49ff37702304cd" +
                "f1dc4c7a9b8f67c73af07fc14422c0be9ae20d"
              ).decodeHex()
          ),
        ),
        BlockHashes(
          arn = arn("aff4://c215ba20-5648-4209-a793-1f918c723610/", "blockhash.sha1"),
          hash = Hash.Sha512(
            (
              "5f487386e32230f282174d197c40a6de4b8d039449a90cf0b720aeb9d213cf337b92a6f0547c5150dd5d1dfcc8" +
                "17e6d5018a2383efec7b6df38015235c9be9e1"
              ).decodeHex()
          ),
        ),
        ZipVolume(
          arn = arn("aff4://685e15cc-d0fb-4dbc-ba47-48117fc77044"),
          creationTime = parseGMT("2016-12-07T03:40:09.126Z"),
          interfaceType = aff4Type("Volume"),
          stored = "Base-Linear.aff4".toPath(),
          contains = listOf(
            arn("aff4://c1a6ab35-d46a-4c37-9bfe-0b3e4f0f1ca3"),
            arn("aff4://c215ba20-5648-4209-a793-1f918c723610"),
            arn("aff4://c21070c3-6d57-4f3b-9276-f83b6bfed5ae"),
            arn("aff4://db69295f-70c3-4e82-9530-a39507f1447b"),
            arn("aff4://427e2078-b010-462b-ba7c-f286b390ba94"),
            arn("aff4://fcbfdce7-4488-4677-abf6-08bc931e195b"),
            arn("aff4://cf853d0b-5589-4c7c-8358-2ca1572b87eb"),
          )
        ),
        Map(
          arn = arn("aff4://fcbfdce7-4488-4677-abf6-08bc931e195b"),
          blockMapHash = Hash.Sha512(
            (
              "c339331791f2018c50247cae1307ea8b0ce1166fac8747c5f4438c364b3d6c56793405afec7eec366205073ed" +
                "9f7e7801556587c87181d83afe356bc9244ccf2"
              ).decodeHex()
          ),
          dependentStream = arn("aff4://c215ba20-5648-4209-a793-1f918c723610"),
          mapGapDefaultStream = aff4Type("Zero"),
          size = 268435456L,
          stored = arn("aff4://685e15cc-d0fb-4dbc-ba47-48117fc77044"),
          target = arn("aff4://cf853d0b-5589-4c7c-8358-2ca1572b87eb"),
          mapHash = Hash.Sha512(
            (
              "7acc88edc1a89a97ac170e140a8dd26ba1caf51b8ac35e4136ca1de57af4e54182009b57124773da717f405a0" +
                "a5f77c2bf366ab8cb3a3d7882053066b92cd303"
              ).decodeHex()
          ),
          mapIdxHash = Hash.Sha512(
            (
              "cc85c72d925186d58a072c1542ba18a6d8b6d7008a1b9adc3bac85841fad3dbfc2c71797029902847e0b4b9bc9" +
                "44ec6c5e3ae7f4e3d115144ef0db978e127a76"
              ).decodeHex()
          ),
          mapPathHash = Hash.Sha512(
            (
              "ce1b4e71d96f17817a7f0f4077851aee8ccc4b624a1043c45b76b7fa567d12578c6ea491cd3cce50b20cbb0136" +
                "db809e56ba43fa3c963c26aac31074e3310f1a"
              ).decodeHex()
          ),
          mapPointHash = Hash.Sha512(
            (
              "2add12a4a27e3167f5c03b0ee364dc6762d705b64963981b3dc5081d16ee1c70d7898b8f4eeb14d70a511755ae" +
                "86e31321cd598db02e659af030c56fbf924b22"
              ).decodeHex()
          ),
        ),
        CaseNotes(
          arn = arn("aff4://427e2078-b010-462b-ba7c-f286b390ba94"),
          caseNumber = "Case ID: 1SR Canonical",
          evidenceNumber = "Drive 1",
          examiner = "Administrator",
          notes = "This is an appended case note",
          stored = arn("aff4://685e15cc-d0fb-4dbc-ba47-48117fc77044"),
          target = arn("aff4://cf853d0b-5589-4c7c-8358-2ca1572b87eb"),
          timestamp = parseGMT("2016-12-07T03:40:09.127Z")
        ),
        CaseNotes(
          arn = arn("aff4://c21070c3-6d57-4f3b-9276-f83b6bfed5ae"),
          caseNumber = "Case ID: 1SR Canonical",
          evidenceNumber = "Drive 1",
          examiner = "Administrator",
          notes = "This is another appended case note",
          stored = arn("aff4://685e15cc-d0fb-4dbc-ba47-48117fc77044"),
          target = arn("aff4://cf853d0b-5589-4c7c-8358-2ca1572b87eb"),
          timestamp = parseGMT("2016-12-07T03:40:14.127Z")
        ),
        CaseDetails(
          arn = arn("aff4://c1a6ab35-d46a-4c37-9bfe-0b3e4f0f1ca3"),
          caseDescription = "Canonical Image Generation Test Case",
          caseName = "Canonical Image Generation",
          examiner = "Administrator",
          stored = arn("aff4://685e15cc-d0fb-4dbc-ba47-48117fc77044"),
          target = arn("aff4://cf853d0b-5589-4c7c-8358-2ca1572b87eb"),
        ),
        TimeStamps(
          arn = arn("aff4://db69295f-70c3-4e82-9530-a39507f1447b"),
          endTime = parseGMT("2016-12-07T03:40:09.28Z"),
          operation = Aff4ImagingOperation.CAPTURE,
          startTime = parseGMT("2016-12-07T03:40:09.131Z"),
          stored = arn("aff4://685e15cc-d0fb-4dbc-ba47-48117fc77044"),
          target = arn("aff4://cf853d0b-5589-4c7c-8358-2ca1572b87eb"),
          timeSource = Aff4TimeSource.SINK,
        ),
      )
  }

  private fun parseGMT(parse: String) = ZonedDateTime.parse(parse).withZoneSameLocal(ZoneId.of("GMT"))

  private fun arn(namespace: String, localName: String) = valueFactory.createIRI(namespace, localName)
  private fun arn(iri: String) = valueFactory.createIRI(iri)

  private fun aff4Type(localName: String) = valueFactory.createIRI("http://aff4.org/Schema#", localName)
}
