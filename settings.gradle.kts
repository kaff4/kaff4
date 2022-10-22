rootProject.name = "engine"

include(
  "core:core-aws",
  "core:core-aws:core-aws-lambda",
  "core:core-aws:core-aws-sqs",
  "core:core-aws:core-aws-s3",
  "core:core-configuration",
  "core:core-guice",
  "core:core-logging",
  "core:core-moshi",
  "core:core-test",
  "sources:sources-upload",
  "sources:sources-enumeration",
  "aff4:aff4-compression-snappy",
  "aff4:aff4-core",
  "aff4:aff4-core:aff4-core-interval-tree",
  "aff4:aff4-core:aff4-core-model",
  "aff4:aff4-core:aff4-core-okio",
  "aff4:aff4-core:aff4-core-test",
  "aff4:aff4-rdf",
  "aff4:aff4-rdf:aff4-rdf-memory",
  "aff4:aff4-reader",
)