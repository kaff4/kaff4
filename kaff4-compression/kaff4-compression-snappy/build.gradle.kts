dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api(Dependencies.JAVAX_INJECT)

  api(project(":kaff4-plugin"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))

  implementation(Dependencies.GUICE)
  implementation(Dependencies.MISK_INJECT)
  implementation("org.xerial.snappy:snappy-java:1.1.8.4")

  implementation(project(":kaff4-core:kaff4-core-guice"))

  testImplementation(project(":kaff4-compression:kaff4-compression-test"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()