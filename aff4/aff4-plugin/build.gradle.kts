dependencies {
  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)

  api(project(":aff4::aff4-core:aff4-core-model:aff4-core-model-api"))
  api(project(":aff4:aff4-core:aff4-core-guice"))
  api(project(":aff4:aff4-rdf:aff4-rdf-api"))

  runtimeOnly(Dependencies.RDF4J_REPOSITORY_SAIL)
}
