dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api(libs.jakarta.inject.api)

  api(project(":kaff4-plugin"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))

  implementation(libs.guice)
  implementation(libs.xerial.snappy)

  implementation(project(":kaff4-core:kaff4-core-guice"))

  testImplementation(libs.assertj)
  testImplementation(libs.junit.juipter.api)

  testImplementation(project(":kaff4-compression:kaff4-compression-test"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))

  testRuntimeOnly(libs.junit.juipter.engine)
}