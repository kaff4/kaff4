plugins {
  id("kaff4.kotlin")
  id("com.jaredsburrows.license")

  id("kaff4.detekt")
}

licenseReport {
  generateTextReport = true
  generateHtmlReport = true
  generateCsvReport = false
  generateJsonReport = false
}