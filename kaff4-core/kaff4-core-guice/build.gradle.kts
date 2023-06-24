dependencies {
  runtimeOnly(kotlin("reflect"))

  api(libs.guice)
  api(libs.javax.inject)
  api(libs.misk.inject)

  implementation(libs.guice.assistedInject)
}