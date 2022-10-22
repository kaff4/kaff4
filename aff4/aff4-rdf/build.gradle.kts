dependencies {
  api(project(":aff4:aff4-core:aff4-core-guice"))
  api(project(":aff4:aff4-core:aff4-core-logging"))

  api("org.eclipse.rdf4j:rdf4j-repository-api:4.0.0")
  api("org.eclipse.rdf4j:rdf4j-query:4.0.0")

  implementation(Dependencies.CAFFIENE)
  implementation(Dependencies.GUICE_ASSISTED_INJECT)

  implementation("org.eclipse.rdf4j:rdf4j-repository-sail:4.0.0")
  implementation("org.eclipse.rdf4j:rdf4j-rio-turtle:4.0.0")

  testImplementation(project(":aff4:aff4-core"))
  testImplementation(project(":aff4:aff4-rdf:aff4-rdf-memory"))
}