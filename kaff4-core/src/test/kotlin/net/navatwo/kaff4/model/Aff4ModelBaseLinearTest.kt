package net.navatwo.kaff4.model

import net.navatwo.kaff4.Aff4ImageTestModule
import net.navatwo.kaff4.BaseLinear
import net.navatwo.kaff4.UnderTest
import net.navatwo.kaff4.io.decode
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.Aff4ImagingOperation
import net.navatwo.kaff4.model.rdf.Aff4Schema
import net.navatwo.kaff4.model.rdf.Aff4TimeSource
import net.navatwo.kaff4.model.rdf.BlockHashes
import net.navatwo.kaff4.model.rdf.CaseDetails
import net.navatwo.kaff4.model.rdf.CaseNotes
import net.navatwo.kaff4.model.rdf.DiskImage
import net.navatwo.kaff4.model.rdf.Hash
import net.navatwo.kaff4.model.rdf.Image
import net.navatwo.kaff4.model.rdf.ImageStream
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.model.rdf.TimeStamps
import net.navatwo.kaff4.model.rdf.ZipVolume
import net.navatwo.kaff4.model.rdf.createArn
import net.navatwo.kaff4.parseZonedDateTime
import net.navatwo.kaff4.satisfies
import net.navatwo.kaff4.streams.compression.SnappyCompression
import net.navatwo.test.GuiceModule
import okio.ByteString.Companion.decodeHex
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Inject

class Aff4ModelBaseLinearTest {
  @GuiceModule
  val module = Aff4ImageTestModule.BaseLinear

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var snappyCompression: SnappyCompression

  @Inject
  @field:UnderTest
  private lateinit var aff4Image: Aff4Image

  @Inject
  @field:UnderTest
  private lateinit var aff4Model: Aff4Model

  private lateinit var containerArns: Set<Aff4Arn>
  private lateinit var diskImageArn: Aff4Arn

  @BeforeEach
  fun setup() {
    diskImageArn = arn("aff4://cf853d0b-5589-4c7c-8358-2ca1572b87eb")
    containerArns = setOf(
      arn("aff4://685e15cc-d0fb-4dbc-ba47-48117fc77044"),
    )
  }

  @Test
  fun `model loads correctly`() {
    assertThat(aff4Image.containers).singleElement().satisfies { container ->
      assertThat(container.metadata).isEqualTo(Aff4Container.ToolMetadata("1.0", "Evimetry 2.2.0"))
      assertThat(container.containerArn).isEqualTo(containerArns.single())
    }

    assertThat(aff4Model.query<ZipVolume>().single()).isEqualTo(
      ZipVolume(
        arn = containerArns.single(),
        creationTime = parseZonedDateTime("2016-12-07T03:40:09.126Z"),
        interfaceType = aff4Type("Volume"),
        stored = "Base-Linear.aff4".toPath(),
        contains = setOf(
          arn("aff4://c1a6ab35-d46a-4c37-9bfe-0b3e4f0f1ca3"),
          arn("aff4://c215ba20-5648-4209-a793-1f918c723610"),
          arn("aff4://c21070c3-6d57-4f3b-9276-f83b6bfed5ae"),
          arn("aff4://db69295f-70c3-4e82-9530-a39507f1447b"),
          arn("aff4://427e2078-b010-462b-ba7c-f286b390ba94"),
          arn("aff4://fcbfdce7-4488-4677-abf6-08bc931e195b"),
          diskImageArn,
        )
      ),
    )
  }

  @Test
  fun `loads images`() {
    val mapArn = arn("aff4://fcbfdce7-4488-4677-abf6-08bc931e195b")
    val imageStreamArn = arn("aff4://c215ba20-5648-4209-a793-1f918c723610")

    assertThat(aff4Model.query<Image>().toSet()).containsExactly(
      Image(
        arn = diskImageArn,
        dataStreams = setOf(mapArn),
        size = 268435456L,
      ),
    )

    assertThat(aff4Model.query<MapStream>().toSet()).containsExactly(
      MapStream(
        arn = mapArn,
        blockMapHash = Hash.Sha512(
          (
            "c339331791f2018c50247cae1307ea8b0ce1166fac8747c5f4438c364b3d6c56793405afec7eec366205073ed" +
              "9f7e7801556587c87181d83afe356bc9244ccf2"
            ).decodeHex()
        ),
        dependentStreams = setOf(imageStreamArn),
        mapGapDefaultStream = aff4Type("Zero"),
        size = 268435456L,
        stored = containerArns.single(),
        target = diskImageArn,
        mapHash = Hash.Sha512.decode(
          "7acc88edc1a89a97ac170e140a8dd26ba1caf51b8ac35e4136ca1de57af4e54182009b57124773da717f405a0" +
            "a5f77c2bf366ab8cb3a3d7882053066b92cd303"
        ),
        mapIdxHash = Hash.Sha512.decode(
          "cc85c72d925186d58a072c1542ba18a6d8b6d7008a1b9adc3bac85841fad3dbfc2c71797029902847e0b4b9bc9" +
            "44ec6c5e3ae7f4e3d115144ef0db978e127a76"
        ),
        mapPathHash = Hash.Sha512.decode(
          "ce1b4e71d96f17817a7f0f4077851aee8ccc4b624a1043c45b76b7fa567d12578c6ea491cd3cce50b20cbb0136" +
            "db809e56ba43fa3c963c26aac31074e3310f1a"
        ),
        mapPointHash = Hash.Sha512.decode(
          "2add12a4a27e3167f5c03b0ee364dc6762d705b64963981b3dc5081d16ee1c70d7898b8f4eeb14d70a511755ae" +
            "86e31321cd598db02e659af030c56fbf924b22"
        ),
      ),
    )

    assertThat(aff4Model.query<ImageStream>().toSet()).containsExactly(
      ImageStream(
        arn = imageStreamArn,
        chunkSize = 32768,
        chunksInSegment = 2048,
        size = 3964928,
        compressionMethod = snappyCompression,
        linearHashes = setOf(
          Hash.Sha1.decode("fbac22cca549310bc5df03b7560afcf490995fbb"),
          Hash.Md5.decode("d5825dc1152a42958c8219ff11ed01a3"),
        ),
        imageStreamHashes = setOf(
          Hash.Sha512.decode(
            "7c909ad458a90ca083cf2d10848fb3aaee7d9ac008605f85aef1ac2db8249973ac7b6716f3250edb80219ff628d" +
              "6fb4873c33c59de0a3e6c7657e234e7ba0db3"
          ),
        ),
        imageStreamIndexHashes = setOf(
          Hash.Sha512.decode(
            "c663bc90d996d2c9699e00dc1ea2c55b3724f1eaca2b92119bb7c764aad222eed321cb00ee67899c027f6837a3bd" +
              "8f789a96adb6e9df51629b3cac0b6f9f0722"
          ),
        ),
        stored = containerArns.single(),
        targets = setOf(mapArn),
        version = 1,
      ),
    )

    assertThat(aff4Model.query<BlockHashes>().toSet()).containsExactly(
      BlockHashes(
        arn = arn("aff4://c215ba20-5648-4209-a793-1f918c723610/", "blockhash.md5"),
        hash = Hash.Sha512.decode(
          "9062f1c9f48438a6875a60b7e1323151e8ff583c8531ca7806d6c29b7d961ceddba8783e8e4c49ff37702304cd" +
            "f1dc4c7a9b8f67c73af07fc14422c0be9ae20d"
        ),
      ),
      BlockHashes(
        arn = arn("aff4://c215ba20-5648-4209-a793-1f918c723610/", "blockhash.sha1"),
        hash = Hash.Sha512.decode(
          "5f487386e32230f282174d197c40a6de4b8d039449a90cf0b720aeb9d213cf337b92a6f0547c5150dd5d1dfcc8" +
            "17e6d5018a2383efec7b6df38015235c9be9e1"
        ),
      ),
    )
  }

  @Suppress("LongMethod")
  @Test
  fun `loads aff4 metadata`() {
    assertThat(aff4Model.query<DiskImage>().toSet()).containsExactly(
      DiskImage(
        arn = diskImageArn,
        size = 268435456L,
        blockSize = 512,
        sectorCount = 524288L,
        acquisitionCompletionState = "Completed Normally",
        diskPartitionTableType = "MBR",
        diskNumberOfPartitions = 1,
        diskDeviceName = "/dev/sdz",
        diskDeviceRole = 0,
        diskDeviceType = "Disk",
        diskFirmware = "1.02a",
        diskInterfaceType = "ATA",
        diskMake = "Seagate",
        diskModel = "ST-506",
        diskSerial = "SGAT5060001234",
      ),
    )

    assertThat(aff4Model.query<CaseNotes>().toSet()).containsExactlyInAnyOrder(
      CaseNotes(
        arn = arn("aff4://427e2078-b010-462b-ba7c-f286b390ba94"),
        caseNumber = "Case ID: 1SR Canonical",
        evidenceNumber = "Drive 1",
        examiner = "Administrator",
        notes = "This is an appended case note",
        stored = containerArns,
        target = diskImageArn,
        timestamp = parseZonedDateTime("2016-12-07T03:40:09.127Z")
      ),
      CaseNotes(
        arn = arn("aff4://c21070c3-6d57-4f3b-9276-f83b6bfed5ae"),
        caseNumber = "Case ID: 1SR Canonical",
        evidenceNumber = "Drive 1",
        examiner = "Administrator",
        notes = "This is another appended case note",
        stored = containerArns,
        target = diskImageArn,
        timestamp = parseZonedDateTime("2016-12-07T03:40:14.127Z")
      ),
    )

    assertThat(aff4Model.query<CaseDetails>().toSet()).containsExactly(
      CaseDetails(
        arn = arn("aff4://c1a6ab35-d46a-4c37-9bfe-0b3e4f0f1ca3"),
        caseDescription = "Canonical Image Generation Test Case",
        caseName = "Canonical Image Generation",
        examiner = "Administrator",
        stored = containerArns,
        target = diskImageArn,
      ),
    )

    assertThat(aff4Model.query<TimeStamps>().toSet()).containsExactly(
      TimeStamps(
        arn = arn("aff4://db69295f-70c3-4e82-9530-a39507f1447b"),
        endTime = parseZonedDateTime("2016-12-07T03:40:09.28Z"),
        operation = Aff4ImagingOperation.CAPTURE,
        startTime = parseZonedDateTime("2016-12-07T03:40:09.131Z"),
        stored = containerArns,
        target = diskImageArn,
        timeSource = Aff4TimeSource.SINK,
      ),
    )
  }

  private fun arn(namespace: String, localName: String) = valueFactory.createArn(namespace, localName)
  private fun arn(iri: String) = valueFactory.createArn(iri)

  private fun aff4Type(localName: String) = valueFactory.createArn(Aff4Schema.SCHEMA, localName)
}
