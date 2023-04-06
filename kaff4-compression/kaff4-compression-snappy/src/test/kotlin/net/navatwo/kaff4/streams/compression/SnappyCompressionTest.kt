package net.navatwo.kaff4.streams.compression

import net.navatwo.test.GuiceModule
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import javax.inject.Inject

class SnappyCompressionTest : BaseCompressionMethodTest() {
  @GuiceModule
  val testModule = Aff4SnappyPlugin

  @Inject
  override lateinit var compressionMethod: SnappyCompression

  override val compressedDataValues = CompressedDataValues(
    compressedSize = 484,
    md5 = "deec7e31e44503f4a685a38780e29ba1",
  )

  @Test
  fun `method == IDENTIFIER`() {
    Assertions.assertThat(compressionMethod.method).isEqualTo(SnappyCompression.IDENTIFIER)
  }
}
