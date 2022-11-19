dependencies {
  implementation(kotlin("reflect"))

  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)
  api(Dependencies.OKIO)
  api(Dependencies.RDF4J_MODEL_API)
  api(project(":interval-tree"))

  api(project(":aff4:aff4-core:aff4-core-guice"))
  api(project(":aff4:aff4-core:aff4-core-model:aff4-core-model-api"))
  api(project(":aff4:aff4-core:aff4-core-model"))
  api(project(":aff4:aff4-core:aff4-core-okio"))

  implementation(Dependencies.GUICE_ASSISTED_INJECT)
  implementation(Dependencies.GUAVA)
  implementation(Dependencies.CHECKER_QUAL)
  implementation(Dependencies.RDF4J_QUERY)
  implementation(Dependencies.RDF4J_REPOSITORY_API)
  implementation(Dependencies.RDF4J_RIO_API)
  implementation(Dependencies.CAFFIENE)
  implementation("io.github.zabuzard.fastcdc4j:fastcdc4j:1.3")

  implementation(project(":aff4:aff4-core:aff4-core-kotlin"))
  implementation(project(":aff4:aff4-rdf"))

  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
  testImplementation(project(":aff4:aff4-compression:aff4-compression-snappy"))
  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
  testImplementation(project(":aff4:aff4-rdf:aff4-rdf-memory"))
}

useJunit5()