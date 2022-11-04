dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api(project(":aff4:aff4-core:aff4-core-guice"))
  api(project(":aff4:aff4-compression:aff4-compression-api"))

  api(Dependencies.GUICE)

  implementation("org.xerial.snappy:snappy-java:1.1.8.4")
}
