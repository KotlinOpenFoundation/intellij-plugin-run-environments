dependencies {
  intellijPlatform {
    bundledModule("intellij.platform.kernel.backend")
    bundledModule("intellij.platform.rpc.backend")
    bundledModule("intellij.platform.backend")
    bundledModule("intellij.platform.ide.impl")

    bundledModule("com.intellij.modules.json")

    compileOnly(libs.kotlin.serialization.core.jvm)
    compileOnly(libs.kotlin.serialization.json.jvm)
  }

  implementation(projects.shared)
}
