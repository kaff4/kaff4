dependencies {
  api(project(":aff4:aff4-core:aff4-core-guice"))
  api(project(":aff4:aff4-rdf"))

  implementation("org.eclipse.rdf4j:rdf4j-sail-memory:4.0.0")
}