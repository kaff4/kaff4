dependencies {
  api("com.google.inject:guice:7.0.0")
  api("javax.inject:javax.inject:1")
  api("com.squareup.misk:misk-inject:0.24.0")

  api(project("::kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-rdf:kaff4-rdf-api"))


  runtimeOnly("org.eclipse.rdf4j:rdf4j-repository-sail:4.0.5")
}
