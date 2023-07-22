plugins {
  id("kaff4.library")
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))

  api(libs.junit.juipter.api)

  implementation(project(":kaff4-compression"))
  implementation(project(":kaff4-core:kaff4-core-test"))

  implementation(libs.assertj)
  implementation(libs.okio)
}