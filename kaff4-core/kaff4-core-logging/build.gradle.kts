dependencies {
  api(libs.log4j.api)

  runtimeOnly(libs.log4j.core)
  runtimeOnly(libs.log4j.slf4j.impl)
}

useJunit5()