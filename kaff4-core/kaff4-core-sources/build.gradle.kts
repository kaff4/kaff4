plugins {
  id("kaff4.library")
}

dependencies {
  implementation(libs.okio)
  
  compileOnly(project(":kaff4-api"))
}