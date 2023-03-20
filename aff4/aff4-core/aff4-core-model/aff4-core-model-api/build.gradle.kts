dependencies {
  api(Dependencies.GUAVA)
  api(Dependencies.OKIO)
  api(Dependencies.RDF4J_MODEL_API)
  api(Dependencies.RDF4J_QUERY)

  implementation(kotlin("reflect"))
}

useJunit5()