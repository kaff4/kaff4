dependencies {
  api(project(":aff4:aff4-core:aff4-core-guice"))
  api(project(":aff4:aff4-rdf:aff4-rdf-api"))
  api(project(":aff4:aff4-plugin-api"))

  implementation(Dependencies.RDF4J_REPOSITORY_SAIL_MEMORY)
}