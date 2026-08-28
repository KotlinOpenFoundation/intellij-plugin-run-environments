dependencies {
  intellijPlatform {
    bundledModule("intellij.platform.frontend")
    bundledModule("fleet.rpc")
    bundledModule("intellij.platform.rpc")

    compileOnly(libs.kotlin.serialization.core.jvm)
    compileOnly(libs.kotlin.serialization.json.jvm)
  }

  implementation(projects.shared)
}
