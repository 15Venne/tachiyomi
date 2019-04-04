plugins {
  id("java-library")
  id("kotlin")
  id("kotlin-kapt")
}

dependencies {
  api(Deps.kotlin.stdlib)
  api(Deps.rxJava)
  api(Deps.okhttp)
  api(Deps.jsoup)
  api(Deps.gson)
  api(Deps.coroutines.core)
  api(Deps.timber.jdk)

  implementation(Deps.toothpick.runtime)
}
