dependencies {
  api(project(":aff4:aff4-plugin"))

  implementation(Dependencies.GUICE)
  implementation(Dependencies.JAVAX_INJECT)
  implementation(Dependencies.RDF4J_SAIL_API)
  implementation(Dependencies.RDF4J_REPOSITORY_SAIL_MEMORY)

  implementation(project(":aff4:aff4-core:aff4-core-guice"))
  implementation(project(":aff4:aff4-rdf:aff4-rdf-api"))
}