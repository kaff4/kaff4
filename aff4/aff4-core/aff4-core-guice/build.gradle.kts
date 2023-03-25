dependencies {
  runtimeOnly(kotlin("reflect"))

  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)

  implementation(Dependencies.GUICE_ASSISTED_INJECT)
}