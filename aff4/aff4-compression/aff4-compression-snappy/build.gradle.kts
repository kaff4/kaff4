dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api(Dependencies.JAVAX_INJECT)

  api(project(":aff4:aff4-plugin"))
  api(project(":aff4:aff4-core:aff4-core-model:aff4-core-model-api"))

  implementation(Dependencies.GUICE)
  implementation("org.xerial.snappy:snappy-java:1.1.8.4")

  implementation(project(":aff4:aff4-core:aff4-core-guice"))
}
