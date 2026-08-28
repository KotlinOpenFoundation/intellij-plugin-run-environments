package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import java.nio.file.Path

/**
 * Where the environment files live, shared by everything that has to agree on it: the source that
 * reads them ([JsonEnvironmentSource]), the service that offers to create the missing ones
 * ([EnvironmentFileService]), and the listeners that refresh when one changes.
 */
object EnvironmentFiles {
  /**
   * Directories a project's run/debug configurations are commonly stored in, relative to a
   * content root: `.idea/runConfigurations` for a directory-based project's shared
   * configurations, `.run` for ones stored as project files. Ordered least to most specific -
   * the content root's own files are read last and win on key collisions.
   */
  private val RUN_CONFIGURATION_DIRECTORIES = listOf(".idea/runConfigurations", ".run")

  /**
   * A directory environment files are looked up in, together with the label disambiguating the
   * environments found there from same-named ones elsewhere ([ContentRoot.qualifier], `null` for
   * the project root itself).
   */
  data class ContentRoot(val path: Path, val qualifier: String?)

  /**
   * The project root plus every module content root, so a multi-module project can ship a
   * different environment set per module.
   */
  fun contentRoots(project: Project): List<ContentRoot> {
    val projectRoot = project.basePath?.let { Path.of(it) }
    val roots = mutableListOf<ContentRoot>()

    if (projectRoot != null) {
      roots += ContentRoot(projectRoot, qualifier = null)
    }

    for (module in ModuleManager.getInstance(project).modules) {
      for (contentRoot in ModuleRootManager.getInstance(module).contentRoots) {
        val path = Path.of(contentRoot.path)
        if (path == projectRoot) continue
        // Gradle-imported modules are often named with a dotted project-path prefix
        // (e.g. "sample-project.order-service") - only the last segment is worth showing.
        roots += ContentRoot(path, qualifier = module.name.substringAfterLast('.'))
      }
    }

    return roots
  }

  /**
   * The directories [root]'s environment files are read from: [root] itself and the
   * [RUN_CONFIGURATION_DIRECTORIES] under it, the least specific first.
   */
  fun scannedDirectories(root: Path): List<Path> =
    // Note the listOf() around `root`: Path is itself an Iterable<Path> of its name elements, so
    // `+ root` would append those segments instead of the directory.
    RUN_CONFIGURATION_DIRECTORIES.map { root.resolve(it) } + listOf(root)

  /** Every directory of every content root of [project], in [scannedDirectories] order. */
  fun scannedDirectories(project: Project): List<Path> =
    contentRoots(project).flatMap { scannedDirectories(it.path) }

  /**
   * The path as shown to the user: relative to the project root when [path] is under it (so
   * `.run/run.env.json` reads as such rather than as a long absolute path), absolute otherwise -
   * a module content root can sit outside the project directory.
   */
  fun presentablePath(project: Project, path: Path): String {
    val projectRoot = project.basePath?.let { Path.of(it) } ?: return path.toString()
    return if (path.startsWith(projectRoot)) {
      projectRoot.relativize(path).joinToString("/")
    } else {
      path.toString()
    }
  }

  /**
   * Whether [path] is an environment file in one of the directories scanned for [project] - the
   * guard on paths that come back from the frontend, which shouldn't be able to have the backend
   * create a file just anywhere.
   */
  fun isScannedEnvironmentFile(project: Project, path: Path): Boolean =
    EnvironmentFileKind.entries.any { it.fileName == path.fileName?.toString() } &&
      scannedDirectories(project).any { it == path.parent }

  /**
   * Runs [onChange] whenever an environment file might have appeared, changed or vanished - a
   * file event on one of them, or a roots change that adds/removes a place they're looked up in.
   * Subscriptions live until [parentDisposable] is disposed.
   */
  fun subscribeToChanges(project: Project, parentDisposable: Disposable, onChange: () -> Unit) {
    val connection = project.messageBus.connect(parentDisposable)
    connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
      override fun after(events: MutableList<out VFileEvent>) {
        if (events.any { it.isEnvironmentFile() }) onChange()
      }
    })
    connection.subscribe(ModuleRootListener.TOPIC, object : ModuleRootListener {
      override fun rootsChanged(event: ModuleRootEvent) = onChange()
    })
  }

  /**
   * A rename's [VFileEvent.getPath] reflects only the file's current (post-rename) name, so a
   * rename away from an environment file name would otherwise go unnoticed - also check the old
   * name for [VFilePropertyChangeEvent] renames.
   */
  private fun VFileEvent.isEnvironmentFile(): Boolean {
    if (path.substringAfterLast('/').isEnvironmentFileName()) return true
    val rename = this as? VFilePropertyChangeEvent ?: return false
    return rename.isRename &&
      (rename.oldPath.substringAfterLast('/').isEnvironmentFileName() ||
        rename.newPath.substringAfterLast('/').isEnvironmentFileName())
  }

  private fun String.isEnvironmentFileName(): Boolean =
    EnvironmentFileKind.entries.any { it.fileName == this }
}
