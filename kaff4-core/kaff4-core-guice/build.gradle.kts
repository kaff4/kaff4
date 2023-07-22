plugins {
  id("kaff4.library")
}

dependencies {
  runtimeOnly(kotlin("reflect"))

  api(libs.guice)
  api(libs.jakarta.inject.api)
  api(libs.misk.inject)

  compileOnly(project(":kaff4-api"))

  implementation(libs.guice.assistedInject)
}