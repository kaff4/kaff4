dependencies {
  compileOnly(kotlin("reflect"))

  api(project(":aff4:aff4-core:aff4-core-model:aff4-core-model-api"))
  api(project(":aff4:aff4-core:aff4-core-okio"))
  api(project(":aff4:aff4-plugin"))
  api(project(":aff4:aff4-rdf"))

  api(Dependencies.OKIO)
  api(Dependencies.RDF4J_MODEL_API)

  implementation(Dependencies.GUICE)
  implementation(Dependencies.JAVAX_INJECT)

  implementation(project(":aff4:aff4-core:aff4-core-guice"))
  implementation(project(":aff4:aff4-rdf:aff4-rdf-api"))

  testImplementation(Dependencies.RDF4J_MODEL)


  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
}

useJunit5()