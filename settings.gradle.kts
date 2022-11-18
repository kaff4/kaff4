rootProject.name = "kaff4"

include(
  "aff4:aff4-compression:aff4-compression-api",
  "aff4:aff4-compression:aff4-compression-snappy",
  "aff4:aff4-core",
  "aff4:aff4-core:aff4-core-kotlin",
  "interval-tree",
  "aff4:aff4-core:aff4-core-model",
  "aff4:aff4-core:aff4-core-okio",
  "aff4:aff4-core:aff4-core-test",
  "aff4:aff4-core:aff4-core-guice",
  "aff4:aff4-core:aff4-core-logging",
  "aff4:aff4-rdf",
  "aff4:aff4-rdf:aff4-rdf-api",
  "aff4:aff4-rdf:aff4-rdf-memory",
  "aff4:aff4-plugin-api",
  "aff4:aff4-reader",
)