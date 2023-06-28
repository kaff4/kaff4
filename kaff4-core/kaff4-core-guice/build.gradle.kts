dependencies {
  runtimeOnly(kotlin("reflect"))

  api(libs.guice)
  api(libs.jakarta.inject.api)
  api(libs.misk.inject)

  implementation(libs.guice.assistedInject)
}