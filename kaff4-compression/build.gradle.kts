dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api(libs.jakarta.inject.api)

  api(project(":kaff4-plugin"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))

  implementation(libs.guice)
  implementation(libs.misk.inject)

  implementation(project(":kaff4-core:kaff4-core-guice"))
  
  testImplementation(project(":kaff4-compression:kaff4-compression-test"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()