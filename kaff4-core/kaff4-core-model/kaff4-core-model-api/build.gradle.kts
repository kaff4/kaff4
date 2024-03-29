plugins {
  id("kaff4.library")
}

dependencies {
  api(libs.guava)
  api(libs.jakarta.inject.api)
  api(libs.okio)
  api(libs.rdf4j.model.api)
  api(libs.rdf4j.query)

  api(project(":kaff4-core:kaff4-core-guice"))

  compileOnly(project(":kaff4-api"))

  implementation(libs.guice)

  implementation(kotlin("reflect"))

  testImplementation(libs.assertj)
  testImplementation(libs.junit.juipter.api)

  testRuntimeOnly(libs.junit.juipter.engine)
}