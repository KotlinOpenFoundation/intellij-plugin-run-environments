dependencies {
  intellijPlatform {
    bundledModule("fleet.rpc")
    bundledModule("intellij.platform.rpc")

    compileOnly(libs.kotlin.serialization.core.jvm)
  }
}
