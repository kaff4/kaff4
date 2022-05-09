package com.github.nava2.logging

// This is intentionally wierd, it pulls the logger by the file name and there's no class named "OtherLoggingTestFile"
// so this will be "com.github.nava2.logging.OtherLoggingTestFileKt" for a logger name
internal val fileLoggerWithoutClass = Logging.getLogger()