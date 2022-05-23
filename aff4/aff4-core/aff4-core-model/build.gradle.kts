dependencies {
  api(project(":aff4:aff4-rdf"))

  api("com.squareup.okio:okio:3.1.0")
  implementation(Dependencies.GUICE)

  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
}