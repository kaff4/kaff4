dependencies {
  implementation(project(":aff4:aff4-core:aff4-core-kotlin"))
  api(project(":aff4:aff4-core:aff4-core-guice"))
  api(project(":aff4:aff4-core:aff4-core-logging"))

  api(Dependencies.RDF4J_REPOSITORY_API)
  api(Dependencies.RDF4J_QUERY)

  implementation(Dependencies.CAFFIENE)
  implementation(Dependencies.GUICE_ASSISTED_INJECT)

  implementation(Dependencies.RDF4J_REPOSITORY_SAIL)
  implementation(Dependencies.RDF4J_RIO_TURTLE)

  testImplementation(project(":aff4:aff4-core"))
  testImplementation(project(":aff4:aff4-rdf:aff4-rdf-memory"))
}