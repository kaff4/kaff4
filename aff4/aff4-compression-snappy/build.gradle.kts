dependencies {
  api(project(":aff4:aff4-core:aff4-core-model"))
  
  api(Dependencies.GUICE)

  implementation("org.xerial.snappy:snappy-java:1.1.8.4")
}