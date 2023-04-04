dependencies {
  implementation(kotlin("reflect"))

  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)
  api(Dependencies.OKIO)
  api(Dependencies.RDF4J_MODEL_API)
  api(Dependencies.KINTERVAL_TREE)

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-core:kaff4-core-model"))
  api(project(":kaff4-core:kaff4-core-okio"))

  implementation(Dependencies.CAFFIENE)
  implementation(Dependencies.CHECKER_QUAL)
  implementation(Dependencies.GUAVA)
  implementation(Dependencies.GUICE_ASSISTED_INJECT)
  implementation(Dependencies.MISK_INJECT)
  implementation(Dependencies.MISK_ACTION_SCOPES)
  implementation(Dependencies.RDF4J_QUERY)
  implementation(Dependencies.RDF4J_REPOSITORY_API)
  implementation(Dependencies.RDF4J_RIO_API)
  implementation("io.github.zabuzard.fastcdc4j:fastcdc4j:1.3")

  implementation(project(":kaff4-core:kaff4-core-kotlin"))
  implementation(project(":kaff4-rdf"))

  testImplementation(Dependencies.JUNIT_JUPITER_PARAMS)

  testImplementation(project(":kaff4-rdf:kaff4-rdf-memory"))
  testImplementation(project(":kaff4-compression:kaff4-compression-snappy"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()
