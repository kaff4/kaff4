rootProject.name = "kaff4"

plugins {
  id("com.gradle.enterprise") version "3.17.2"
  id("io.github.gradle.gradle-enterprise-conventions-plugin") version "0.10.0"
}

include(
  "kaff4-api",
  "kaff4-api:kaff4-api-features",
  "kaff4-cli",
  "kaff4-compression:kaff4-compression-lz4",
  "kaff4-compression:kaff4-compression-snappy",
  "kaff4-compression:kaff4-compression-test",
  "kaff4-core",
  "kaff4-core:kaff4-core-guice",
  "kaff4-core:kaff4-core-kotlin",
  "kaff4-core:kaff4-core-logging",
  "kaff4-core:kaff4-core-model",
  "kaff4-core:kaff4-core-model:kaff4-core-model-api",
  "kaff4-core:kaff4-core-okio",
  "kaff4-core:kaff4-core-sources",
  "kaff4-core:kaff4-core-test",
  "kaff4-rdf",
  "kaff4-rdf:kaff4-rdf-api",
  "kaff4-rdf:kaff4-rdf-memory",
)

gradleEnterprise {
  buildScan {
    publishAlways()
    
    capture {
      isTaskInputFiles = true
    }

    buildScanPublished {
      println("Build Scan: ${buildScanUri}")
    }
  }
}