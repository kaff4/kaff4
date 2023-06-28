package net.navatwo.kaff4.model

import jakarta.inject.Inject
import net.navatwo.kaff4.Aff4ImageTestModule
import net.navatwo.kaff4.BaseLinearStriped
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
import net.navatwo.kaff4.streams.compression.SnappyCompression
import net.navatwo.test.GuiceModule
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Aff4ModelStripedBaseLinearTest {
  @GuiceModule
  val module = Aff4ImageTestModule.BaseLinearStriped

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
    diskImageArn = arn("aff4://951b3e29-6549-4266-8e81-3f88ddba61ae")
    containerArns = setOf(
      arn("aff4://7cbb47d0-b04c-42bc-8c04-87b7782739ad"),
      arn("aff4://51725cd9-3769-4be7-a8ab-94e3ea62bf9a"),
    )
  }

  @Test
  fun `model loads correctly`() {
    assertThat(aff4Image.containers).hasSize(2)
      .allSatisfy { container ->
        assertThat(container.metadata).isEqualTo(Aff4Container.ToolMetadata("1.0", "Evimetry 2.2.0"))
        assertThat(container.containerArn).isIn(containerArns)
      }

    val expectedZipVolumes = setOf(
      ZipVolume(
        arn = arn("aff4://7cbb47d0-b04c-42bc-8c04-87b7782739ad"),
        creationTime = parseZonedDateTime("2016-12-07T03:40:12.185Z"),
        interfaceType = aff4Type("Volume"),
        stored = "Base-Linear_1.aff4".toPath(),
        contains = setOf(
          arn("aff4://1311a313-e27a-4756-8bb5-22c984061270"),
          arn("aff4://a04a9189-5e92-4024-a577-37d6cfa72594"),
          arn("aff4://2dd04819-73c8-40e3-a32b-fdddb0317eac"),
          arn("aff4://a393456b-1c6f-48f2-aefd-45157d997493"),
          arn("aff4://951b3e29-6549-4266-8e81-3f88ddba61ae"),
          arn("aff4://0adda6d6-97c5-4842-8981-dec2a3373215"),
          arn("aff4://b33a5803-ef89-4463-94ce-f265ec7eb9f1"),
        ),
      ),
      ZipVolume(
        arn = arn("aff4://51725cd9-3769-4be7-a8ab-94e3ea62bf9a"),
        creationTime = parseZonedDateTime("2016-12-07T03:40:12.185Z"),
        interfaceType = aff4Type("Volume"),
        stored = "Base-Linear_2.aff4".toPath(),
        contains = setOf(
          arn("aff4://363ac10c-8d8d-4905-ac25-a14aaddd8a41"),
          arn("aff4://b33a5803-ef89-4463-94ce-f265ec7eb9f1"),
          arn("aff4://951b3e29-6549-4266-8e81-3f88ddba61ae"),
          arn("aff4://0adda6d6-97c5-4842-8981-dec2a3373215"),
          arn("aff4://1311a313-e27a-4756-8bb5-22c984061270"),
          arn("aff4://a393456b-1c6f-48f2-aefd-45157d997493"),
          arn("aff4://3bf0bd14-1ef9-4185-8b0a-2c7d511b4d30"),
        )
      ),
    ).associateBy { it.arn }

    assertThat(aff4Model.query<ZipVolume>().toSet()).allSatisfy { zipVolume ->
      val expectedZipVolume = expectedZipVolumes.getValue(zipVolume.arn)
      assertThat(zipVolume).isEqualTo(expectedZipVolume)
    }
  }

  @Suppress("LongMethod")
  @Test
  fun `loads images`() {
    assertThat(aff4Model.query<Image>().toSet()).containsExactly(
      Image(
        arn = diskImageArn,
        dataStreams = setOf(
          arn("aff4://2dd04819-73c8-40e3-a32b-fdddb0317eac"),
          arn("aff4://363ac10c-8d8d-4905-ac25-a14aaddd8a41"),
        ),
        size = 268435456L,
      ),
    )

    val expectedMapStreams = setOf(
      MapStream(
        arn = arn("aff4://2dd04819-73c8-40e3-a32b-fdddb0317eac"),
        dependentStreams = setOf(
          arn("aff4://a04a9189-5e92-4024-a577-37d6cfa72594"),
          arn("aff4://3bf0bd14-1ef9-4185-8b0a-2c7d511b4d30"),
        ),
        mapGapDefaultStream = aff4Type("Zero"),
        mapHash = Hash.Sha512.decode(
          "b97b60419b686934c5f1bf6ba05e8fa386ad35371543ba5c938d586e578bdbfba4c72a2b8d081fc9382" +
            "fc93cf3a48622f7390719b9c0bd1849265a934f1f97ca"
        ),
        blockMapHash = Hash.Sha512.decode(
          "904c68e4240071a2057f40b1da4328c5c93232924ad6714ab5d6aa27504ec10e387efa89380b42ea4f596" +
            "dcdf1f085330a50f30091d88ebdc8a4a781047fd2d9"
        ),
        mapIdxHash = Hash.Sha512.decode(
          "def3021bead1a260f6ba96f63032bd897e55c36b63056d950acfc872001fd0e03c0d2e066fd8ee50607d4" +
            "0bbf92458bd1a468729ee49a57baebc5c636a78e6f7"
        ),
        mapPathHash = Hash.Sha512.decode(
          "2b0801ec5c04583af00adaf1887d10eedf281962707d28e8d42dc4e2eb4176759fe75c867b0f4fb0e5f326411" +
            "8fc9fa53ee95e0d33f3502cf4fa69f9650cac30"
        ),
        mapPointHash = Hash.Sha512.decode(
          "87df2c13aeafb35ae7abf83ff4575bd4a52090ea941eb94153e80a3f0f73d72c1924986d7862cd4ba736086d5" +
            "fe54264f51577112c45ee58ef652a099aa48d8d"
        ),
        size = 268435456,
        stored = arn("aff4://7cbb47d0-b04c-42bc-8c04-87b7782739ad"),
        target = diskImageArn,
      ),
      MapStream(
        arn = arn("aff4://363ac10c-8d8d-4905-ac25-a14aaddd8a41"),
        dependentStreams = setOf(
          arn("aff4://a04a9189-5e92-4024-a577-37d6cfa72594"),
          arn("aff4://3bf0bd14-1ef9-4185-8b0a-2c7d511b4d30"),
        ),
        mapGapDefaultStream = aff4Type("Zero"),
        mapHash = Hash.Sha512.decode(
          "417a9b650e3552652a0f060698c0e9997c7a6be4495d25b214b787d88140457eeb2fd78108" +
            "104bfc4250a6591826106841fa03946ae598265877b3aa4cfc5951"
        ),
        blockMapHash = Hash.Sha512.decode(
          "1a9618d0d2c8099224a4876f9470d394070bad137e23bc24c23bb42f99c0fd18c7f8c16924" +
            "d31cdf11ff91de0ab165e80a5b5110675c25310d72245145512941"
        ),
        mapIdxHash = Hash.Sha512.decode(
          "def3021bead1a260f6ba96f63032bd897e55c36b63056d950acfc872001fd0e03c0d2e066fd8ee50607d4" +
            "0bbf92458bd1a468729ee49a57baebc5c636a78e6f7"
        ),
        mapPathHash = Hash.Sha512.decode(
          "5dbc0e63b346b0b7a79fd6e5820eeb13e7d83a0a8559ca9612ca2f5f656a938475a6d9b1372" +
            "0eb56cc9e00a5210de9468ea4d51dda905fdd3ef71f3d1416738b"
        ),
        mapPointHash = Hash.Sha512.decode(
          "5550fe7d157d77622da1db2625f8e0cf6eda06ef0a386015314d0625c80cc2f426c9ab99bd2" +
            "14609194dd7d7dbacac31e865fbe398861a8f01b5e72ae4d41fd1"
        ),
        size = 268435456,
        stored = arn("aff4://51725cd9-3769-4be7-a8ab-94e3ea62bf9a"),
        target = diskImageArn,
      ),
    )

    assertThat(aff4Model.query<MapStream>().toSet()).containsExactlyInAnyOrderElementsOf(expectedMapStreams)

    assertThat(aff4Model.query<ImageStream>().toSet()).containsExactlyInAnyOrder(
      ImageStream(
        arn = arn("aff4://3bf0bd14-1ef9-4185-8b0a-2c7d511b4d30"),
        chunkSize = 32768,
        chunksInSegment = 2048,
        size = 1966080,
        compressionMethod = snappyCompression,
        linearHashes = setOf(
          Hash.Sha1.decode("5d14518149402e3935930b389564c9dc8c674fd1"),
          Hash.Md5.decode("8eaf5dd1e12e4dd8cc9c68be51660e4d"),
        ),
        imageStreamHashes = setOf(
          Hash.Sha512.decode(
            "30dfba8269539ee364361d17b4518c32cd367079aa99ff7ee96918ec027ecbceff1334f1f2" +
              "ed2f1046ef433c6ceaf644b7dd55401e7dda46ad1393be73ee24a8"
          ),
        ),
        imageStreamIndexHashes = setOf(
          Hash.Sha512.decode(
            "09a5148b36ac629eb48af0f60263a7dcb81e186908b50544561954acd32810f94a19554d5f9" +
              "06363079a03e4186ee9fe73860fbd0e87a2e55a31dc1dafd79e07"
          ),
        ),
        stored = arn("aff4://51725cd9-3769-4be7-a8ab-94e3ea62bf9a"),
        targets = expectedMapStreams.map { it.arn }.toSet(),
        version = 1,
      ),
      ImageStream(
        arn = arn("aff4://a04a9189-5e92-4024-a577-37d6cfa72594"),
        chunkSize = 32768,
        chunksInSegment = 2048,
        size = 1998848,
        compressionMethod = snappyCompression,
        linearHashes = setOf(
          Hash.Sha1.decode("00412c53666cd74c81d5703c77d865145dde1082"),
          Hash.Md5.decode("b20122bc5e70fafb582820a25f895646"),
        ),
        imageStreamHashes = setOf(
          Hash.Sha512.decode(
            "574e12d53b6a609ad7340cbd7359243fe058876381cd82d6357da19052ed89007aa6e2e635" +
              "87ef8605c9de41691cd12689257e513af8c30a6777f6840c6d76b7"
          ),
        ),
        imageStreamIndexHashes = setOf(
          Hash.Sha512.decode(
            "2364325d0822af6bbd5221813ac04d957f39c652de4a8151ebd8bea4b571f1a5f2dde0ba7a2" +
              "dc274a936705b2e08dc9d6d957cf24ba7e7304cdd5e8aa19a8127"
          ),
        ),
        stored = arn("aff4://7cbb47d0-b04c-42bc-8c04-87b7782739ad"),
        targets = expectedMapStreams.map { it.arn }.toSet(),
        version = 1,
      ),
    )

    assertThat(aff4Model.query<BlockHashes>().toSet()).containsExactlyInAnyOrder(
      BlockHashes(
        arn = arn("aff4://a04a9189-5e92-4024-a577-37d6cfa72594/", "blockhash.md5"),
        hash = Hash.Sha512.decode(
          "4697e941ff7cb7eea6f8762480eb609d16b9a446af3385316b5ffd074b81cad1d4b33bb7881b29bcc" +
            "27db5b548fcd3f51926b7cced206162ac5649645b11d4ba"
        ),
      ),
      BlockHashes(
        arn = arn("aff4://a04a9189-5e92-4024-a577-37d6cfa72594/", "blockhash.sha1"),
        hash = Hash.Sha512.decode(
          "1af482ce0f7237d2cd8bdda38d6550e4a42878e8d8a1854e85dd8beb5caa3567f6c0b96f76f6a455a4" +
            "d021e506e29fbce358f0170edcdcd03e3b15b4a4b3953f"
        ),
      ),
      BlockHashes(
        arn = arn("aff4://3bf0bd14-1ef9-4185-8b0a-2c7d511b4d30/", "blockhash.md5"),
        hash = Hash.Sha512.decode(
          "782c8fbefbd69e30f1fa73a32ae66340dbe735373484cb71922cedbedd6a4a2401d66d962efe6959498" +
            "8ac9ba298b017de46826b77d4f539a8b6ef7c0e8b5d6e"
        ),
      ),
      BlockHashes(
        arn = arn("aff4://3bf0bd14-1ef9-4185-8b0a-2c7d511b4d30/", "blockhash.sha1"),
        hash = Hash.Sha512.decode(
          "983d89b2baa8d431f471cecb3dc7106aae9d79d9d37e0f5ada6eb3fec78e2e1355d5b6388f0ee5cbb8b2" +
            "01ec621553655c0c6a8c99bd8151c11ae9ca1011ebfd"
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
        arn = arn("aff4://a393456b-1c6f-48f2-aefd-45157d997493"),
        caseNumber = "Case ID: 1SR Canonical",
        evidenceNumber = "Drive 1",
        examiner = "Administrator",
        notes = "This is an appended case note",
        stored = containerArns,
        target = diskImageArn,
        timestamp = parseZonedDateTime("2016-12-07T03:40:12.186Z"),
      ),
      CaseNotes(
        arn = arn("aff4://b33a5803-ef89-4463-94ce-f265ec7eb9f1"),
        caseNumber = "Case ID: 1SR Canonical",
        evidenceNumber = "Drive 1",
        examiner = "Administrator",
        notes = "This is another appended case note",
        stored = containerArns,
        target = diskImageArn,
        timestamp = parseZonedDateTime("2016-12-07T03:40:17.186Z"),
      ),
    )

    assertThat(aff4Model.query<CaseDetails>().toSet()).containsExactly(
      CaseDetails(
        arn = arn("aff4://0adda6d6-97c5-4842-8981-dec2a3373215"),
        caseDescription = "Canonical Image Generation Test Case",
        caseName = "Canonical Image Generation",
        examiner = "Administrator",
        stored = containerArns,
        target = diskImageArn,
      ),
    )

    assertThat(aff4Model.query<TimeStamps>().toSet()).containsExactly(
      TimeStamps(
        arn = arn("aff4://1311a313-e27a-4756-8bb5-22c984061270"),
        startTime = parseZonedDateTime("2016-12-07T03:40:12.190Z"),
        endTime = parseZonedDateTime("2016-12-07T03:40:12.339Z"),
        operation = Aff4ImagingOperation.CAPTURE,
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
