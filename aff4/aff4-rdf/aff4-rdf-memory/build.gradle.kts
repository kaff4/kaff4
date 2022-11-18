dependencies {
  api(project(":aff4:aff4-core:aff4-core-guice"))
  api(project(":aff4:aff4-rdf"))

  implementation(Dependencies.RDF4J_REPOSITORY_SAIL_MEMORY)
}