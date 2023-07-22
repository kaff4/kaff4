plugins {
  id("kaff4.library")
}

dependencies {
  api(kotlin("stdlib"))

  compileOnly(project(":kaff4-api"))
}