plugins {
  id("kaff4.library")
}

dependencies {
  implementation(kotlin("reflect"))

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model"))
  api(project(":kaff4-core:kaff4-core-okio"))

  api(libs.assertj)
  api(libs.guice)
  api(libs.jakarta.inject.api)
  api(libs.junit.juipter.api)
  api(libs.okio)

  implementation(libs.misk.inject)
  implementation(libs.misk.actionscopes)

  implementation(project(":kaff4-core"))
  implementation(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  implementation(project(":kaff4-core:kaff4-core-sources"))
  implementation(project(":kaff4-rdf:kaff4-rdf-memory"))
}
