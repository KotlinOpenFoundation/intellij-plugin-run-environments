package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.platform.eel.fs.EelFiles
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class EnvironmentFileServiceTest : BasePlatformTestCase() {
  // createFile/openFile hop onto Dispatchers.EDT internally - running the test body itself on the
  // EDT (BasePlatformTestCase's default) would deadlock waiting for that hop back onto itself.
  override fun runInDispatchThread(): Boolean = false

  // The light fixture's project root is reused across test methods in this class, and
  // createFile/openFile write straight to disk via java.nio, bypassing myFixture's own tracking -
  // clean up what a test wrote so it doesn't leak into the next one, regardless of run order.
  override fun tearDown() {
    try {
      Files.deleteIfExists(projectRoot().resolve(EnvironmentFileKind.BASE.fileName))
      Files.deleteIfExists(projectRoot().resolve(EnvironmentFileKind.PRIVATE.fileName))
    } finally {
      super.tearDown()
    }
  }

  private fun projectRoot(): Path = Path.of(project.basePath!!)

  fun `test createFile writes the default template at the project root and opens it`() = runBlocking {
    val service = EnvironmentFileService(project)
    try {
      service.createFile(EnvironmentFileKind.BASE)

      val path = projectRoot().resolve(EnvironmentFileKind.BASE.fileName)
      assertTrue(Files.isRegularFile(path))
      @Suppress("UnstableApiUsage")
      assertTrue(EelFiles.readString(path).contains("\"dev\""))
    } finally {
      Disposer.dispose(service)
    }
  }

  fun `test createFile does nothing beyond opening it if the file already exists`() = runBlocking {
    val path = projectRoot().resolve(EnvironmentFileKind.BASE.fileName)
    Files.createDirectories(path.parent)
    path.writeText("{}")

    val service = EnvironmentFileService(project)
    try {
      service.createFile(EnvironmentFileKind.BASE)

      @Suppress("UnstableApiUsage")
      assertEquals("{}", EelFiles.readString(path))
    } finally {
      Disposer.dispose(service)
    }
  }

  fun `test createFile does nothing when the project has no base path`() = runBlocking {
    // The default (template) project isn't backed by a directory - basePath is null there.
    val service = EnvironmentFileService(ProjectManager.getInstance().defaultProject)
    try {
      service.createFile(EnvironmentFileKind.BASE)
    } finally {
      Disposer.dispose(service)
    }
  }

  fun `test openFile refuses a path outside the scanned directories`() = runBlocking {
    val outsidePath = projectRoot().resolveSibling("elsewhere").resolve(EnvironmentFileKind.BASE.fileName)

    val service = EnvironmentFileService(project)
    try {
      service.openFile(outsidePath)

      assertFalse(Files.exists(outsidePath))
    } finally {
      Disposer.dispose(service)
    }
  }

  fun `test openFile logs a warning and does nothing when the file can't be created`() = runBlocking {
    // ".run" is one of the scanned directories - making it an ordinary file instead of a directory
    // means creating anything under it genuinely fails, unlike the "already exists" case above.
    val runDirAsFile = projectRoot().resolve(".run")
    runDirAsFile.writeText("not a directory")
    val path = runDirAsFile.resolve(EnvironmentFileKind.BASE.fileName)

    val service = EnvironmentFileService(project)
    try {
      service.openFile(path)

      assertFalse(Files.exists(path))
    } finally {
      Disposer.dispose(service)
      Files.deleteIfExists(runDirAsFile)
    }
  }

  fun `test openFile creates and opens a path inside the scanned directories`() = runBlocking {
    val path = projectRoot().resolve(EnvironmentFileKind.PRIVATE.fileName)

    val service = EnvironmentFileService(project)
    try {
      service.openFile(path)

      assertTrue(Files.isRegularFile(path))
    } finally {
      Disposer.dispose(service)
    }
  }
}
