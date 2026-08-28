package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class MissingFileKindsTest {
  @get:Rule
  val temporaryFolder: TemporaryFolder = TemporaryFolder()

  private val root: Path get() = temporaryFolder.root.toPath()

  @Test
  fun `reports both kinds as missing when no directory holds one`() {
    assertEquals(
      listOf(EnvironmentFileKind.BASE, EnvironmentFileKind.PRIVATE),
      EnvironmentFileService.missingFileKinds(EnvironmentFiles.scannedDirectories(root))
    )
  }

  @Test
  fun `reports only the private file as missing once the base file exists`() {
    writeFile("run.env.json")

    assertEquals(
      listOf(EnvironmentFileKind.PRIVATE),
      EnvironmentFileService.missingFileKinds(EnvironmentFiles.scannedDirectories(root))
    )
  }

  @Test
  fun `counts a file stored next to the run configurations as present`() {
    writeFile(".run/run.env.json")
    writeFile(".idea/runConfigurations/run.private.env.json")

    assertEquals(
      emptyList<EnvironmentFileKind>(),
      EnvironmentFileService.missingFileKinds(EnvironmentFiles.scannedDirectories(root))
    )
  }

  @Test
  fun `counts a file in another content root as present`() {
    writeFile("order-service/run.env.json")

    assertEquals(
      listOf(EnvironmentFileKind.PRIVATE),
      EnvironmentFileService.missingFileKinds(
        EnvironmentFiles.scannedDirectories(root) + EnvironmentFiles.scannedDirectories(root.resolve("order-service"))
      )
    )
  }

  private fun writeFile(relativePath: String) {
    val file = root.resolve(relativePath)
    file.parent.createDirectories()
    file.writeText("{}")
  }
}
