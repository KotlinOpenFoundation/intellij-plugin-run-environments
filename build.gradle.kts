import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure
import org.jetbrains.changelog.Changelog.OutputType.HTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware.PluginInstallationTarget.BOTH
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain

plugins {
  `jvm-toolchains`
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.plugin.serialization) apply false
  alias(libs.plugins.intellij.platform)
  alias(libs.plugins.intellij.platform.module) apply false
  alias(libs.plugins.rpc) apply false
  alias(libs.plugins.changelog)
  alias(libs.plugins.kover)
  alias(libs.plugins.git.version)
}

description =
  """
  IntelliJ Platform plugin: a toolbar selector that injects run.env.json-defined
  environment variables into run configurations.
  """.trimIndent()

val gitVersion: String = runCatching {
  @Suppress("UNCHECKED_CAST")
  val versionDetails = extra["versionDetails"] as Closure<VersionDetails>
  val details = versionDetails()
  if (details.isCleanTag) {
    details.lastTag
  } else {
    bumpDevVersion(
      details.lastTag,
      runCatching { details.branchName }.getOrNull()?.startsWith("release/") == true
    )
  }
}.getOrNull() ?: "1.0.0-SNAPSHOT"
version = gitVersion

@Suppress("AvoidApplyPluginMethod")
subprojects {
  version = rootProject.version

  apply(plugin = rootProject.libs.plugins.kotlin.jvm.get().pluginId)
  apply(plugin = rootProject.libs.plugins.kotlin.plugin.serialization.get().pluginId)
  apply(plugin = rootProject.libs.plugins.intellij.platform.module.get().pluginId)
  apply(plugin = rootProject.libs.plugins.rpc.get().pluginId)
  apply(plugin = rootProject.libs.plugins.kover.get().pluginId)
}

val service = project.extensions.getByType<JavaToolchainService>()
val customLauncher = service.launcherFor {
  languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
  vendor.set(JvmVendorSpec.ADOPTIUM)
}

allprojects {
  kotlin {
    compilerOptions.freeCompilerArgs.addAll(
      "-Xjsr305=strict",
      "-Xcontext-sensitive-resolution",
      // Without this, Kotlin's default "all-compatibility" jvm-default mode generates a synthetic
      // override in every class implementing a platform interface for EACH of its default methods
      // (calling the real default via invokespecial) - including deprecated ones like
      // GradleExecutionHelperExtension.prepareForExecution, which the plugin never calls itself but
      // the verifier still flags as deprecated API usage. "all" relies on real JVM default methods
      // with no compatibility bridges, so no such synthetic calls exist to flag.
      "-jvm-default=no-compatibility"
    )
  }

  tasks {
    withType<UsesKotlinJavaToolchain> {
      kotlinJavaToolchain.toolchain.use(customLauncher)
    }
  }
}

dependencies {
  // Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
  intellijPlatform {
    intellijIdea(libs.versions.idea.get())

    pluginModule(implementation(projects.shared))
    pluginModule(implementation(projects.frontend))
    pluginModule(implementation(projects.backend))
    pluginModule(implementation(projects.backendGradle))
    pluginModule(implementation(projects.backendJava))

    testFramework(TestFrameworkType.Platform)
  }

  testImplementation(libs.junit)
  testImplementation(libs.mockito.core)

  // Merges every module's classes into the root project's coverage reports, measured by the tests
  // in src/test - the modules have no test sources of their own.
  kover(projects.shared)
  kover(projects.frontend)
  kover(projects.backend)
  kover(projects.backendGradle)
  kover(projects.backendJava)
}

kover {
  reports {
    filters {
      excludes {
        // Compiler-generated: kotlinx.serialization's `$serializer` objects and the RPC plugin's
        // `$_generated_` client stubs and descriptors - neither is code anybody writes nor tests.
        classes($$$"*$$serializer", $$"*$_generated_*")
      }
    }

    verify {
      rule {
        minBound(80)
      }
    }
  }
}

intellijPlatform {
  splitMode = true
  pluginInstallationTarget = BOTH

  pluginVerification {
    ides {
      recommended()
    }
  }

  publishing {
    token.set(providers.environmentVariable("JETBRAINS_PUBLISH_TOKEN"))
    channels.set(providers.gradleProperty("publishChannel").map { listOf(it) }.orElse(listOf("stable")))
  }

  signing {
    certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
    privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
    password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
  }
}

changelog {
  repositoryUrl.set("https://github.com/KotlinOpenFoundation/intellij-plugin-run-environments")
}

tasks {
  check {
    finalizedBy(koverBinaryReport, koverXmlReport, koverHtmlReport, koverPrintCoverage)
  }

  // Every module keeps its own `internal` declarations testable from here: all tests live in this
  // project's src/test rather than per-module test source sets (the modules have no test sources
  // of their own), so this project's test compilation needs friend access to each module's main one.
  named<KotlinCompile>("compileTestKotlin") {
    // Friend paths must match the actual jars each module resolves to on the classpath (the plain
    // "-base" jar, not the raw classes directory), or Kotlin won't recognize them as friends.
    friendPaths.from(
      listOf(":shared", ":frontend", ":backend", ":backend-gradle", ":backend-java")
        .map { project(it).tasks.named("jar") }
    )
  }

  patchPluginXml {
    pluginVersion = project.version.toString()

    // Renders CHANGELOG.md into the <change-notes> - edit CHANGELOG.md, not plugin.xml, to update this.
    val changelogExtension = changelog
    changeNotes = provider {
      with(changelogExtension) {
        val currentVersion = getOrNull(project.version.toString()) ?: getUnreleased()
        renderItem(currentVersion, HTML)
      }
    }
  }
}

/**
 * Predicts a snapshot version, given the last real tag and whether the current branch is a release branch.
 *
 * @param lastTag the name of last git tag
 * @param isReleaseBranch whether the current branch is a release branch
 */
fun bumpDevVersion(
  lastTag: String?,
  isReleaseBranch: Boolean
): String {
  if (lastTag == null) return "1.0.0-SNAPSHOT"
  val parts = lastTag.removePrefix("v")
    .split('-').first()
    .split(".")
  require(parts.size == 3) { "Invalid tag: $lastTag" }
  var (major, minor, patch) = parts
  if (isReleaseBranch) {
    patch += 1
  } else {
    minor += 1
  }
  return "$major.$minor.$patch-SNAPSHOT"
}
