dependencies {
  api(project(":aff4:aff4-rdf"))
  api(project(":aff4:aff4-compression:aff4-compression-api"))
  api(project(":aff4:aff4-core:aff4-core-okio"))

  api(Dependencies.OKIO)
  api(Dependencies.RDF4J_MODEL_API)
  api(Dependencies.GUICE)

  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
}