dependencies {
  api(Dependencies.GUAVA)
  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)
  api(Dependencies.OKIO)
  api(Dependencies.RDF4J_MODEL_API)
  api(Dependencies.RDF4J_QUERY)

  api(project(":aff4:aff4-core:aff4-core-guice"))

  implementation(kotlin("reflect"))
}

useJunit5()