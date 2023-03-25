dependencies {
  implementation(kotlin("reflect"))

  api(project(":aff4:aff4-core:aff4-core-guice"))
  api(project(":aff4:aff4-core:aff4-core-model:aff4-core-model-api"))
  api(project(":aff4:aff4-rdf:aff4-rdf-api"))

  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)
  api(Dependencies.RDF4J_MODEL_API)
  api(Dependencies.RDF4J_REPOSITORY_API)
  api(Dependencies.RDF4J_RIO_API)
  api(Dependencies.RDF4J_QUERY)

  implementation(project(":guice-action-scoped"))
  implementation(Dependencies.APACHE_COMMONS_LANG)
  implementation(Dependencies.CAFFIENE)
  implementation(Dependencies.CHECKER_QUAL)
  implementation(Dependencies.GUAVA)
  implementation(Dependencies.GUICE_ASSISTED_INJECT)
  implementation(Dependencies.RDF4J_REPOSITORY_SAIL)
  implementation(Dependencies.RDF4J_RIO_TURTLE)
  implementation(Dependencies.RDF4J_SAIL_API)

  implementation(project(":aff4:aff4-core:aff4-core-kotlin"))

  testImplementation(project(":aff4:aff4-core"))
  testImplementation(project(":aff4:aff4-core:aff4-core-model"))
  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
  testImplementation(project(":aff4:aff4-rdf:aff4-rdf-memory"))
}

useJunit5()