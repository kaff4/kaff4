dependencies {
  runtimeOnly(kotlin("reflect"))

  api("com.google.inject:guice:7.0.0")
  api("javax.inject:javax.inject:1")
  api("com.squareup.misk:misk-inject:0.24.0")

  implementation("com.google.inject.extensions:guice-assistedinject:7.0.0")
}