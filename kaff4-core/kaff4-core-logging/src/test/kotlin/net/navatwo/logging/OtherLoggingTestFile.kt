package net.navatwo.logging

// This is intentionally wierd, it pulls the logger by the file name and there's no class named "OtherLoggingTestFile"
// so this will be "net.navatwo.logging.OtherLoggingTestFileKt" for a logger name
internal val fileLoggerWithoutClass = Logging.getLogger()
