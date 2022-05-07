package com.github.nava2.logging

import org.apache.logging.log4j.LogManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

private val fileLogger by Logging

class LoggingKtTest {
  private val logger by Logging

  @Test
  fun `logging delegate pulls correct class`() {
    assertThat(logger).isEqualTo(LogManager.getLogger(LoggingKtTest::class.java))

    val testLogger0 = TestLogger0()
    assertThat(testLogger0.logger).isEqualTo(LogManager.getLogger(TestLogger0::class.java))
  }

  @Test
  fun `file loggers pull the file name and uses class name if it exists`() {
    assertThat(fileLogger).isEqualTo(LogManager.getLogger(LoggingKtTest::class.java))
  }

  @Test
  fun `file loggers pull the file name and falls back to ${FILENAME}Kt naming`() {
    val expectedPackageName = LoggingKtTest::class.java.packageName
    assertThat(fileLoggerWithoutClass)
      .isEqualTo(LogManager.getLogger("$expectedPackageName.OtherLoggingTestFileKt"))
  }
}

private class TestLogger0 {
  val logger by Logging
}
