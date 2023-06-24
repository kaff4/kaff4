dependencies {
  api(libs.guava)
  api(libs.javax.inject)
  api(libs.okio)
  api(libs.rdf4j.model.api)
  api(libs.rdf4j.query)

  api(project(":kaff4-core:kaff4-core-guice"))

  implementation(libs.guice)
  implementation(libs.misk.inject)

  implementation(kotlin("reflect"))
}

useJunit5()