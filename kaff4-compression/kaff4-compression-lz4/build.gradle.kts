dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api(libs.jakarta.inject.api)

  api(project(":kaff4-api:kaff4-api-features"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))

  compileOnly(project(":kaff4-api"))

  implementation(libs.guava)
  implementation(libs.guice)
  implementation(libs.misk.inject)
  implementation(libs.commons.compress)

  implementation(project(":kaff4-core:kaff4-core-guice"))
  implementation(project(":kaff4-compression"))

  testImplementation(libs.assertj)
  testImplementation(libs.junit.juipter.api)

  testImplementation(project(":kaff4-compression:kaff4-compression-test"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))

  testRuntimeOnly(libs.junit.juipter.engine)
}