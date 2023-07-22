plugins {
  id("kaff4.library")
}

dependencies {
  compileOnly(kotlin("reflect"))

  api(project(":kaff4-api:kaff4-api-features"))
  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-core:kaff4-core-okio"))
  api(project(":kaff4-rdf"))

  api(libs.jakarta.inject.api)
  api(libs.okio)
  api(libs.rdf4j.model.api)

  implementation(libs.guice)

  compileOnly(project(":kaff4-api"))

  testImplementation(libs.assertj)
  testImplementation(libs.junit.juipter.api)
  testImplementation(libs.rdf4j.model)

  testImplementation(project(":kaff4-core:kaff4-core-model"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))

  testRuntimeOnly(libs.junit.juipter.engine)
}