package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileLink
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class JsonEnvironmentSourceTest : BasePlatformTestCase() {
  private val source = JsonEnvironmentSource()

  fun `test returns nothing when no environment files exist`() {
    assertEmpty(environments())
  }

  fun `test reads environment names from run env json`() {
    writeEnvFile("run.env.json", """{"dev": {}, "prod": {}}""")

    assertSameElements(environments(), EnvironmentInfo("dev"), EnvironmentInfo("prod"))
  }

  fun `test reads variables for each environment`() {
    writeEnvFile("run.env.json", """{"dev": {"HOST": "dev.example.com", "PORT": "8080"}}""")

    assertSameElements(
      environments(),
      EnvironmentInfo("dev", variables = mapOf("HOST" to "dev.example.com", "PORT" to "8080"))
    )
  }

  fun `test ignores non-string variable values`() {
    writeEnvFile("run.env.json", """{"dev": {"HOST": "dev.example.com", "PORT": 8080, "DEBUG": true}}""")

    assertSameElements(
      environments(),
      EnvironmentInfo("dev", variables = mapOf("HOST" to "dev.example.com"))
    )
  }

  fun `test ignores a nested object or array variable value`() {
    writeEnvFile(
      "run.env.json",
      """{"dev": {"HOST": "dev.example.com", "NESTED": {"a": "b"}, "LIST": [1, 2]}}"""
    )

    assertSameElements(
      environments(),
      EnvironmentInfo("dev", variables = mapOf("HOST" to "dev.example.com"))
    )
  }

  fun `test merges names from base and private files without duplicating shared ones`() {
    writeEnvFile("run.env.json", """{"dev": {}, "prod": {}}""")
    writeEnvFile("run.private.env.json", """{"local": {}, "prod": {}}""")

    assertSameElements(
      environments(),
      EnvironmentInfo("dev"), EnvironmentInfo("prod"), EnvironmentInfo("local")
    )
  }

  fun `test private file variables take precedence over base file variables for the same environment`() {
    writeEnvFile("run.env.json", """{"prod": {"HOST": "base.example.com", "REGION": "eu"}}""")
    writeEnvFile("run.private.env.json", """{"prod": {"HOST": "private.example.com"}}""")

    assertSameElements(
      environments(),
      EnvironmentInfo("prod", variables = mapOf("HOST" to "private.example.com", "REGION" to "eu"))
    )
  }

  fun `test ignores the reserved shared key`() {
    writeEnvFile("run.env.json", $$"""{"$shared": {}, "dev": {}}""")

    assertSameElements(environments(), EnvironmentInfo("dev"))
  }

  fun `test reads environment files stored next to the run configurations`() {
    writeEnvFile(".run/run.env.json", """{"dev": {"HOST": "dev.example.com"}}""")
    writeEnvFile(".idea/runConfigurations/run.env.json", """{"prod": {"HOST": "prod.example.com"}}""")

    assertSameElements(
      environments(),
      EnvironmentInfo("dev", variables = mapOf("HOST" to "dev.example.com")),
      EnvironmentInfo("prod", variables = mapOf("HOST" to "prod.example.com"))
    )
  }

  fun `test merges an environment defined in several scanned directories without duplicating it`() {
    writeEnvFile(".idea/runConfigurations/run.env.json", """{"dev": {"REGION": "eu"}}""")
    writeEnvFile(".run/run.env.json", """{"dev": {"PORT": "8080"}}""")
    writeEnvFile("run.env.json", """{"dev": {"HOST": "dev.example.com"}}""")

    assertSameElements(
      environments(),
      EnvironmentInfo("dev", variables = mapOf("REGION" to "eu", "PORT" to "8080", "HOST" to "dev.example.com"))
    )
  }

  fun `test content root variables take precedence over the ones next to the run configurations`() {
    writeEnvFile(".idea/runConfigurations/run.env.json", """{"dev": {"HOST": "idea.example.com"}}""")
    writeEnvFile(".run/run.env.json", """{"dev": {"HOST": "run.example.com"}}""")
    writeEnvFile("run.env.json", """{"dev": {"HOST": "root.example.com"}}""")

    assertSameElements(
      environments(),
      EnvironmentInfo("dev", variables = mapOf("HOST" to "root.example.com"))
    )
  }

  fun `test private file next to the run configurations overrides its base file`() {
    writeEnvFile(".run/run.env.json", """{"dev": {"HOST": "base.example.com", "REGION": "eu"}}""")
    writeEnvFile(".run/run.private.env.json", """{"dev": {"HOST": "private.example.com"}}""")

    assertSameElements(
      environments(),
      EnvironmentInfo("dev", variables = mapOf("HOST" to "private.example.com", "REGION" to "eu"))
    )
  }

  fun `test links an environment to the files of the directory it was detected in`() {
    writeEnvFile("run.env.json", """{"dev": {}}""")

    assertEquals(
      listOf(
        fileLink(EnvironmentFileKind.BASE, "run.env.json", exists = true),
        fileLink(EnvironmentFileKind.PRIVATE, "run.private.env.json", exists = false)
      ),
      source.listEnvironments(project).single().files
    )
  }

  fun `test links point next to the run configurations for an environment detected there`() {
    writeEnvFile(".run/run.private.env.json", """{"dev": {}}""")

    assertEquals(
      listOf(
        fileLink(EnvironmentFileKind.BASE, ".run/run.env.json", exists = false),
        fileLink(EnvironmentFileKind.PRIVATE, ".run/run.private.env.json", exists = true)
      ),
      source.listEnvironments(project).single().files
    )
  }

  fun `test links cover every directory defining the environment, most specific first`() {
    writeEnvFile("run.env.json", """{"dev": {}}""")
    writeEnvFile(".run/run.env.json", """{"dev": {}}""")

    assertEquals(
      listOf(
        fileLink(EnvironmentFileKind.BASE, "run.env.json", exists = true),
        fileLink(EnvironmentFileKind.PRIVATE, "run.private.env.json", exists = false),
        fileLink(EnvironmentFileKind.BASE, ".run/run.env.json", exists = true),
        fileLink(EnvironmentFileKind.PRIVATE, ".run/run.private.env.json", exists = false)
      ),
      source.listEnvironments(project).single().files
    )
  }

  fun `test does not link a directory that does not define the environment`() {
    writeEnvFile("run.env.json", """{"dev": {}}""")
    writeEnvFile(".run/run.env.json", """{"prod": {}}""")

    assertSameElements(
      source.listEnvironments(project).single { it.name == "dev" }.files.map { it.presentablePath },
      "run.env.json", "run.private.env.json"
    )
  }

  fun `test ignores a file that is not valid json`() {
    writeEnvFile("run.env.json", "not json")

    assertEmpty(environments())
  }

  /**
   * [BasePlatformTestCase] reuses the same on-disk light-project directory across test methods,
   * so files written by one test would otherwise leak into the next.
   */
  override fun tearDown() {
    try {
      for (directory in listOf("", ".run", ".idea/runConfigurations")) {
        for (name in listOf("run.env.json", "run.private.env.json")) {
          Files.deleteIfExists(projectRoot().resolve(directory).resolve(name))
        }
      }
    } finally {
      super.tearDown()
    }
  }

  private fun projectRoot(): Path = Path.of(project.basePath!!)

  /**
   * The environments as the rest of the plugin sees them, minus the file links - those have their
   * own tests, and spelling out absolute paths in every other expectation would drown them.
   */
  private fun environments(): List<EnvironmentInfo> =
    source.listEnvironments(project).map { it.copy(files = emptyList()) }

  private fun fileLink(kind: EnvironmentFileKind, presentablePath: String, exists: Boolean): EnvironmentFileLink =
    EnvironmentFileLink(
      kind = kind,
      path = projectRoot().resolve(presentablePath).toString(),
      presentablePath = presentablePath,
      exists = exists
    )

  /**
   * [BasePlatformTestCase]'s light project's [com.intellij.openapi.project.Project.getBasePath]
   * isn't backed by a real directory until something creates one - [JsonEnvironmentSource] reads
   * plain NIO paths, so the environment files need to land on actual disk at that path.
   *
   * [relativePath] is resolved against the project root, so a test can place a file in one of the
   * scanned run/debug configuration directories as well.
   */
  private fun writeEnvFile(relativePath: String, content: String) {
    val file = projectRoot().resolve(relativePath)
    Files.createDirectories(file.parent)
    file.writeText(content)
  }
}
