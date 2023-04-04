dependencies {
  runtimeOnly(kotlin("reflect"))

  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)
  api(Dependencies.MISK_INJECT)

  implementation(Dependencies.GUICE_ASSISTED_INJECT)
}