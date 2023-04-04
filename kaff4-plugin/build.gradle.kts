dependencies {
  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)
  api(Dependencies.MISK_INJECT)

  api(project("::kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-rdf:kaff4-rdf-api"))


  runtimeOnly(Dependencies.RDF4J_REPOSITORY_SAIL)
}
