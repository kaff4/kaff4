dependencies {
  implementation(kotlin("reflect"))

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-rdf:kaff4-rdf-api"))

  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)
  api(Dependencies.RDF4J_MODEL_API)
  api(Dependencies.RDF4J_REPOSITORY_API)
  api(Dependencies.RDF4J_RIO_API)
  api(Dependencies.RDF4J_QUERY)

  implementation(Dependencies.APACHE_COMMONS_LANG)
  implementation(Dependencies.CAFFIENE)
  implementation(Dependencies.CHECKER_QUAL)
  implementation(Dependencies.GUAVA)
  implementation(Dependencies.GUICE_ASSISTED_INJECT)
  implementation(Dependencies.MISK_INJECT)
  implementation(Dependencies.MISK_ACTION_SCOPES)
  implementation(Dependencies.RDF4J_REPOSITORY_SAIL)
  implementation(Dependencies.RDF4J_RIO_TURTLE)
  implementation(Dependencies.RDF4J_SAIL_API)

  implementation(project(":kaff4-core:kaff4-core-kotlin"))

  testImplementation(project(":kaff4-core"))
  testImplementation(project(":kaff4-core:kaff4-core-model"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
  testImplementation(project(":kaff4-rdf:kaff4-rdf-memory"))
}

useJunit5()