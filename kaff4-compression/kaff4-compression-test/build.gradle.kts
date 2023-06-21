dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))

  api("org.junit.jupiter:junit-jupiter-api:5.9.3")

  implementation(project(":kaff4-compression"))
  implementation(project(":kaff4-core:kaff4-core-test"))

  implementation("org.assertj:assertj-core:3.22.0")
  implementation("com.squareup.okio:okio:3.3.0")
}