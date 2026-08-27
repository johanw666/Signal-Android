plugins {
  id("signal-library")
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "org.signal.passwordmanager"

  buildFeatures {
    compose = true
  }
}

dependencies {
  lintChecks(project(":lintchecks"))

  implementation(project(":core:util"))

  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.compat)

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
}
