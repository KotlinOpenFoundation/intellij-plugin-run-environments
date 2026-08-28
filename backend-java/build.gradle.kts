import org.jetbrains.intellij.platform.gradle.TestFrameworkType

dependencies {
  intellijPlatform {
    bundledModule("intellij.platform.backend")
    bundledPlugin("com.intellij.java")

    compileOnly(libs.kotlin.serialization.core.jvm)
    compileOnly(libs.kotlin.serialization.json.jvm)

    testFramework(TestFrameworkType.Platform)
  }

  implementation(projects.shared)
  implementation(projects.backend)

  testImplementation(libs.junit)
}
