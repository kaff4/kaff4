dependencies {
  implementation(kotlin("reflect"))

  api("com.google.inject:guice:5.1.0")
  api("javax.inject:javax.inject:1")
  api("com.squareup.okio:okio:3.3.0")
  api("org.eclipse.rdf4j:rdf4j-model-api:4.3.2")
  api("net.navatwo:kinterval-tree:0.1.0")

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-core:kaff4-core-model"))
  api(project(":kaff4-core:kaff4-core-okio"))

    implementation("com.github.ben-manes.caffeine:caffeine:3.1.0")
  implementation("org.checkerframework:checker-qual:3.21.4")
  implementation("com.google.guava:guava:32.0.1-jre")
  implementation("com.google.inject.extensions:guice-assistedinject:5.1.0")
  implementation("com.squareup.misk:misk-inject:0.24.0")
  implementation("com.squareup.misk:misk-action-scopes:0.25.0-20230405.1913-51e097f")
  implementation("org.eclipse.rdf4j:rdf4j-query:4.3.2")
  implementation("org.eclipse.rdf4j:rdf4j-repository-api:4.0.5")
  implementation("org.eclipse.rdf4j:rdf4j-rio-api:4.3.2")
  implementation("io.github.zabuzard.fastcdc4j:fastcdc4j:1.3")

  implementation(project(":kaff4-core:kaff4-core-kotlin"))
  implementation(project(":kaff4-rdf"))

  testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.1")

  testImplementation(project(":kaff4-rdf:kaff4-rdf-memory"))
  testImplementation(project(":kaff4-compression"))
  testImplementation(project(":kaff4-compression:kaff4-compression-lz4"))
  testImplementation(project(":kaff4-compression:kaff4-compression-snappy"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()
