import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  id("org.jetbrains.intellij.platform.settings") version "2.18.1"
}

rootProject.name = "kotlinopenfoundation-intellij-plugin-run-environments"

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    mavenCentral()
    intellijPlatform {
      defaultRepositories()
    }
  }
}

include("shared", "frontend", "backend", "backend-gradle", "backend-java")
