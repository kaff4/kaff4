dependencies {
  api(project(":kaff4-plugin"))

  implementation(Dependencies.GUICE)
  implementation(Dependencies.JAVAX_INJECT)
  implementation(Dependencies.MISK_INJECT)
  implementation(Dependencies.RDF4J_SAIL_API)
  implementation(Dependencies.RDF4J_REPOSITORY_SAIL_MEMORY)

  implementation(project(":kaff4-core:kaff4-core-guice"))
  implementation(project(":kaff4-rdf:kaff4-rdf-api"))
  implementation(project(":kaff4-rdf"))
}