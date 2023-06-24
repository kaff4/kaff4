dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api(libs.javax.inject)

  api(project(":kaff4-plugin"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))

  implementation(libs.guice)
  implementation(libs.misk.inject)
  implementation(libs.commons.compress)

  implementation(project(":kaff4-core:kaff4-core-guice"))
  implementation(project(":kaff4-compression"))
  
  testImplementation(project(":kaff4-compression:kaff4-compression-test"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()