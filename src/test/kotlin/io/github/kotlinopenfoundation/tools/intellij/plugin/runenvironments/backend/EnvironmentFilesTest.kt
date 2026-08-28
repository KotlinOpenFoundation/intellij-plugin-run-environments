package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import java.nio.file.Path

class EnvironmentFilesTest : BasePlatformTestCase() {
  fun `test presentablePath returns a path relative to the project root`() {
    val path = Path.of(project.basePath!!).resolve(".run").resolve(EnvironmentFileKind.BASE.fileName)

    assertEquals(".run/run.env.json", EnvironmentFiles.presentablePath(project, path))
  }

  fun `test presentablePath returns the absolute path when outside the project root`() {
    val outside = Path.of(project.basePath!!).resolveSibling("elsewhere").resolve(EnvironmentFileKind.BASE.fileName)

    assertEquals(outside.toString(), EnvironmentFiles.presentablePath(project, outside))
  }

  fun `test isScannedEnvironmentFile is true for a known file name in a scanned directory`() {
    val path = Path.of(project.basePath!!).resolve(EnvironmentFileKind.BASE.fileName)

    assertTrue(EnvironmentFiles.isScannedEnvironmentFile(project, path))
  }

  fun `test isScannedEnvironmentFile is false for an unrelated file name`() {
    val path = Path.of(project.basePath!!).resolve("notes.txt")

    assertFalse(EnvironmentFiles.isScannedEnvironmentFile(project, path))
  }

  fun `test isScannedEnvironmentFile is false outside the scanned directories`() {
    val path = Path.of(project.basePath!!).resolveSibling("elsewhere").resolve(EnvironmentFileKind.BASE.fileName)

    assertFalse(EnvironmentFiles.isScannedEnvironmentFile(project, path))
  }

  fun `test subscribeToChanges notifies when a file is renamed into an environment file name`() {
    val file = myFixture.addFileToProject("notes.txt", "").virtualFile
    var notified = false

    EnvironmentFiles.subscribeToChanges(project, testRootDisposable) { notified = true }

    // Renaming lands the file on run.env.json, in the shared project's own root - delete it again
    // once observed, so it doesn't leak into every other test that scans this project.
    WriteCommandAction.runWriteCommandAction(project) {
      file.rename(this, EnvironmentFileKind.BASE.fileName)
    }
    try {
      assertTrue(notified)
    } finally {
      WriteCommandAction.runWriteCommandAction(project) { file.delete(this) }
    }
  }

  fun `test subscribeToChanges notifies when a file is renamed away from an environment file name`() {
    val file = myFixture.addFileToProject(EnvironmentFileKind.BASE.fileName, "{}").virtualFile
    var notified = false

    EnvironmentFiles.subscribeToChanges(project, testRootDisposable) { notified = true }

    try {
      WriteCommandAction.runWriteCommandAction(project) {
        file.rename(this, "notes.txt")
      }

      assertTrue(notified)
    } finally {
      WriteCommandAction.runWriteCommandAction(project) { file.delete(this) }
    }
  }

  fun `test subscribeToChanges does not notify when renaming between two unrelated names`() {
    val file = myFixture.addFileToProject("notes.txt", "").virtualFile
    var notified = false

    EnvironmentFiles.subscribeToChanges(project, testRootDisposable) { notified = true }

    try {
      WriteCommandAction.runWriteCommandAction(project) {
        file.rename(this, "notes2.txt")
      }

      assertFalse(notified)
    } finally {
      WriteCommandAction.runWriteCommandAction(project) { file.delete(this) }
    }
  }
}
