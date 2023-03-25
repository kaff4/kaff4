object Versions {
  const val JVM_BYTECODE_TARGET = 17
  const val KOTLIN = "1.8.10"
  const val DETEKT = "1.22.0"
}

repositories {
  mavenCentral()
}

dependencies {
  runtimeOnly(kotlin("reflect"))

  api("com.google.inject:guice:5.1.0")

  implementation("javax.inject:javax.inject:1")
  implementation("com.google.guava:guava:31.1-jre")

  // TODO Move to library, too
  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
  testImplementation("org.assertj:assertj-core:3.24.2")

  val junitVersion = "5.9.2"
  testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}
