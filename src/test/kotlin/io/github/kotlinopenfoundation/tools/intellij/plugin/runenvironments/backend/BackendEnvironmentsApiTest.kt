package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.projectId
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import fleet.util.UID
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path

class BackendEnvironmentsApiTest : BasePlatformTestCase() {
  // createEnvironmentFile/openEnvironmentFile hop onto Dispatchers.EDT internally (through
  // EnvironmentFileService) - running the test body itself on the EDT (BasePlatformTestCase's
  // default) would deadlock waiting for that hop back onto itself.
  override fun runInDispatchThread(): Boolean = false

  // createEnvironmentFile writes straight to disk via java.nio, bypassing myFixture's own tracking
  // - the light project's root is shared with the rest of the suite, so it needs cleanup. Deleting
  // through the VFS (rather than raw NIO) fires a real change event, so EnvironmentFileService's
  // own cached missingFiles state - shared with every other test - doesn't go stale either.
  override fun tearDown() {
    try {
      val path = Path.of(project.basePath!!).resolve(EnvironmentFileKind.BASE.fileName)
      val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)
      if (virtualFile != null) {
        WriteAction.run<Throwable> { virtualFile.delete(this) }
      }
    } finally {
      super.tearDown()
    }
  }

  private val api = BackendEnvironmentsApi()

  /** Not registered to any real project - exercises the "project not found" branch of every method. */
  @Suppress("UnstableApiUsage")
  private val unknownProjectId = ProjectId(UID.random())

  fun `test getEnvironments returns the project's own environments`() = runBlocking {
    assertEquals(
      project.service<EnvironmentAggregatorService>().environments.first(),
      api.getEnvironments(project.projectId()).first()
    )
  }

  fun `test getEnvironments returns nothing for an unknown project`() = runBlocking {
    assertEquals(emptyList<Any>(), api.getEnvironments(unknownProjectId).toList())
  }

  fun `test setSelectedEnvironment updates the project's SelectedEnvironmentService`() = runBlocking {
    api.setSelectedEnvironment(project.projectId(), "dev")

    assertEquals("dev", project.service<SelectedEnvironmentService>().selectedKey)
  }

  fun `test setSelectedEnvironment does nothing for an unknown project`() = runBlocking {
    api.setSelectedEnvironment(unknownProjectId, "dev")
  }

  fun `test isInjectionSupported returns the project's own support flow`() = runBlocking {
    assertEquals(
      project.service<SelectedRunConfigurationSupportService>().isSupportedFlow.first(),
      api.isInjectionSupported(project.projectId()).first()
    )
  }

  fun `test isInjectionSupported returns nothing for an unknown project`() = runBlocking {
    assertEquals(emptyList<Any>(), api.isInjectionSupported(unknownProjectId).toList())
  }

  fun `test getMissingEnvironmentFiles returns the project's own missing files`() = runBlocking {
    assertEquals(
      project.service<EnvironmentFileService>().missingFiles.first(),
      api.getMissingEnvironmentFiles(project.projectId()).first()
    )
  }

  fun `test getMissingEnvironmentFiles returns nothing for an unknown project`() = runBlocking {
    assertEquals(emptyList<Any>(), api.getMissingEnvironmentFiles(unknownProjectId).toList())
  }

  fun `test createEnvironmentFile delegates to the project's EnvironmentFileService`() = runBlocking {
    api.createEnvironmentFile(project.projectId(), EnvironmentFileKind.BASE)

    assertTrue(Files.isRegularFile(Path.of(project.basePath!!).resolve(EnvironmentFileKind.BASE.fileName)))
  }

  fun `test createEnvironmentFile does nothing for an unknown project`() = runBlocking {
    api.createEnvironmentFile(unknownProjectId, EnvironmentFileKind.BASE)
  }

  fun `test openEnvironmentFile delegates to the project's EnvironmentFileService`() = runBlocking {
    val path = Path.of(project.basePath!!).resolve(EnvironmentFileKind.BASE.fileName)
    Files.createDirectories(path.parent)
    Files.writeString(path, "{}")

    // Resolves through the real EnvironmentFileService.openFile - completing without throwing is
    // the whole point: the path is one of the scanned directories, so it isn't refused.
    api.openEnvironmentFile(project.projectId(), path.toString())
  }

  fun `test openEnvironmentFile does nothing for an unknown project`() = runBlocking {
    api.openEnvironmentFile(unknownProjectId, "/does/not/matter")
  }
}
