dependencies {
  compileOnly(kotlin("reflect"))

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-core:kaff4-core-okio"))
  api(project(":kaff4-plugin"))
  api(project(":kaff4-rdf"))

  api(libs.guice)
  api(libs.jakarta.inject.api)
  api(libs.okio)
  api(libs.rdf4j.model.api)

  implementation(libs.misk.inject)

  testImplementation(libs.rdf4j.model)

  testImplementation(project(":kaff4-core:kaff4-core-model"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()
