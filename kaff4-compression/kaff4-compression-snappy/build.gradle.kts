dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api("javax.inject:javax.inject:1")

  api(project(":kaff4-plugin"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))

  implementation("com.google.inject:guice:7.0.0")
  implementation("com.squareup.misk:misk-inject:0.24.0")
  implementation("org.xerial.snappy:snappy-java:1.1.10.1")

  implementation(project(":kaff4-core:kaff4-core-guice"))

  testImplementation(project(":kaff4-compression:kaff4-compression-test"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()