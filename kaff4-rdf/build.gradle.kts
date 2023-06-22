dependencies {
  implementation(kotlin("reflect"))

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-rdf:kaff4-rdf-api"))

  api("com.google.inject:guice:5.1.0")
  api("javax.inject:javax.inject:1")
  api("org.eclipse.rdf4j:rdf4j-model-api:4.0.5")
  api("org.eclipse.rdf4j:rdf4j-repository-api:4.0.5")
  api("org.eclipse.rdf4j:rdf4j-rio-api:4.0.5")
  api("org.eclipse.rdf4j:rdf4j-query:4.3.2")

  implementation("org.apache.commons:commons-lang3:3.12.0")
  implementation("com.github.ben-manes.caffeine:caffeine:3.1.0")
  implementation("org.checkerframework:checker-qual:3.21.4")
  implementation("com.google.guava:guava:32.0.1-jre")
  implementation("com.google.inject.extensions:guice-assistedinject:5.1.0")
  implementation("com.squareup.misk:misk-inject:0.24.0")
  implementation("com.squareup.misk:misk-action-scopes:0.25.0-20230405.1913-51e097f")
  implementation("org.eclipse.rdf4j:rdf4j-repository-sail:4.0.5")
  implementation("org.eclipse.rdf4j:rdf4j-rio-turtle:4.0.5")
  implementation("org.eclipse.rdf4j:rdf4j-sail-api:4.0.5")

  implementation(project(":kaff4-core:kaff4-core-kotlin"))

  testImplementation(project(":kaff4-core"))
  testImplementation(project(":kaff4-core:kaff4-core-model"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
  testImplementation(project(":kaff4-rdf"))
  testImplementation(project(":kaff4-rdf:kaff4-rdf-memory"))
}

useJunit5()