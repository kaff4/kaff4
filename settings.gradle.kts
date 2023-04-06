rootProject.name = "kaff4"

include(
  "kaff4-compression:kaff4-compression-snappy",
  "kaff4-compression:kaff4-compression-test",
  "kaff4-core",
  "kaff4-core:kaff4-core-kotlin",
  "kaff4-core:kaff4-core-model",
  "kaff4-core:kaff4-core-model:kaff4-core-model-api",
  "kaff4-core:kaff4-core-okio",
  "kaff4-core:kaff4-core-test",
  "kaff4-core:kaff4-core-guice",
  "kaff4-core:kaff4-core-logging",
  "kaff4-plugin",
  "kaff4-rdf",
  "kaff4-rdf:kaff4-rdf-api",
  "kaff4-rdf:kaff4-rdf-memory",
  "kaff4-reader",
)