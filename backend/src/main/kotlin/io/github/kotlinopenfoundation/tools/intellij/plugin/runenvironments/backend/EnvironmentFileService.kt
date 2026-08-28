package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Tracks which environment files the project doesn't have yet, and creates them on request - the
 * frontend offers a "create" entry in the environment combo box for every [missingFiles] kind, so
 * a project with no `run.env.json` at all isn't a dead end.
 *
 * A kind counts as missing only when no directory the JSON source scans has it, so an existing
 * file in a module or next to the run/debug configurations doesn't get shadowed by a second one
 * created at the project root.
 */
@Service(PROJECT)
class EnvironmentFileService(
  private val project: Project
) : Disposable {
  private val _missingFiles = MutableStateFlow(computeMissingFiles())

  /** The file kinds that exist in none of the scanned directories, in [EnvironmentFileKind] order. */
  val missingFiles: StateFlow<List<EnvironmentFileKind>> = _missingFiles.asStateFlow()

  init {
    EnvironmentFiles.subscribeToChanges(project, parentDisposable = this) { refresh() }
  }

  /**
   * Creates [kind]'s file at the project root - the one place that exists in every project, and
   * the same default the HTTP Client uses - and opens it in the editor so the user lands directly
   * in the environment they're about to define. Does nothing beyond opening it if the file turned
   * up in the meantime. Use [openFile] for a file next to an already detected environment.
   */
  suspend fun createFile(kind: EnvironmentFileKind) {
    val projectRoot = project.basePath?.let { Path.of(it) } ?: return
    openOrCreate(projectRoot.resolve(kind.fileName))
  }

  /**
   * Opens the environment file at [path], creating it first if it doesn't exist - the call behind
   * an environment's own edit/create links, which point at the directory that environment was
   * detected in.
   *
   * [path] comes back from the frontend, so it's only honored for an environment file in one of
   * the scanned directories.
   */
  suspend fun openFile(path: Path) {
    if (!EnvironmentFiles.isScannedEnvironmentFile(project, path)) {
      thisLogger().warn("Refusing to open $path: not an environment file in a scanned directory")
      return
    }
    openOrCreate(path)
  }

  private suspend fun openOrCreate(path: Path) {
    val file = withContext(Dispatchers.IO) {
      try {
        path.parent?.let { Files.createDirectories(it) }
        Files.writeString(path, TEMPLATE, StandardOpenOption.CREATE_NEW)
      } catch (e: Exception) {
        // An existing file is fine - it's what the user wanted to end up with either way; anything
        // else (no write permission, ...) leaves nothing to open.
        if (!Files.isRegularFile(path)) {
          thisLogger().warn("Failed to create $path", e)
          return@withContext null
        }
      }
      VfsUtil.findFile(path, true)
    } ?: return

    withContext(Dispatchers.EDT) {
      FileEditorManager.getInstance(project).openFile(file, true)
    }
  }

  private fun refresh() {
    _missingFiles.value = computeMissingFiles()
  }

  private fun computeMissingFiles(): List<EnvironmentFileKind> =
    missingFileKinds(EnvironmentFiles.scannedDirectories(project))

  override fun dispose() = Unit

  companion object {
    /**
     * The [EnvironmentFileKind]s none of [directories] holds - a kind found in any one of them is
     * present as far as the JSON source is concerned, wherever that directory sits.
     */
    fun missingFileKinds(directories: List<Path>): List<EnvironmentFileKind> =
      EnvironmentFileKind.entries.filter { kind ->
        directories.none { Files.isRegularFile(it.resolve(kind.fileName)) }
      }

    /**
     * A single empty environment rather than an empty object: it shows up in the combo box right
     * away, so the file the user just created is visibly connected to the selector it feeds.
     */
    private val TEMPLATE = """
      {
        "dev": {}
      }
    """.trimIndent() + "\n"
  }
}
