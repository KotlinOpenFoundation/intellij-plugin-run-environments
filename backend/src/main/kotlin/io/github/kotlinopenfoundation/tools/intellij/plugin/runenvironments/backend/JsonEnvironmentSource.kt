package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.eel.fs.EelFiles
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentSource
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileLink
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import java.nio.file.Path

/**
 * Reads environments from `run.env.json` (checked in) and `run.private.env.json` (local
 * overrides/additions, meant to be gitignored), looked up in every directory
 * [EnvironmentFiles.scannedDirectories] names: the project root and every module content root, so
 * a multi-module project can ship a different environment set per module, plus the directories
 * run/debug configurations are commonly stored in, for teams who would rather keep the
 * environment files next to the configurations they feed than at the root itself.
 *
 * File shape mirrors the HTTP Client environment file: a flat JSON object whose top-level keys
 * are the environment names, and each value is itself a flat object of variable name/value pairs,
 * e.g. `{"dev": {"HOST": "dev.example.com"}, "uat": {}}`. Non-string variable values are ignored.
 * A `$shared` top-level key (also HTTP Client convention) is ignored rather than treated as an
 * environment. `run.private.env.json` entries with the same name as one in `run.env.json` don't
 * produce a duplicate - both files contribute to the same environment, with the private file's
 * variables taking precedence over the base file's on key collisions - and neither does the same
 * environment name appearing in two of the scanned directories, with the content root's own files
 * taking precedence over the ones stored alongside the run/debug configurations.
 */
class JsonEnvironmentSource : EnvironmentSource {
  companion object {
    /** Reserved key for shared variables in the HTTP Client env file format; not an environment. */
    private const val SHARED_KEY = $$"$shared"
  }

  override fun listEnvironments(project: Project): List<EnvironmentInfo> =
    EnvironmentFiles.contentRoots(project).flatMap { readContentRoot(project, it) }

  /**
   * Reads a content root's own directory and the run/debug configuration directories under it as
   * a single environment set: an environment defined in more than one of them is one
   * [EnvironmentInfo] whose variables are merged, not a duplicate entry.
   */
  private fun readContentRoot(project: Project, contentRoot: EnvironmentFiles.ContentRoot): List<EnvironmentInfo> {
    val directories = EnvironmentFiles.scannedDirectories(contentRoot.path)
    val scanned = directories.map { readEnvironmentDirectory(it) }
    return scanned.flatMap { it.keys }.distinct().map { name ->
      EnvironmentInfo(
        name = name,
        qualifier = contentRoot.qualifier,
        variables = scanned.fold(emptyMap()) { merged, environments -> merged + environments[name].orEmpty() },
        files = fileLinks(project, directories.zip(scanned), name)
      )
    }
  }

  /**
   * Both file kinds of every directory that defines [name], most specific directory first, so the
   * UI can offer to edit the files the environment actually comes from - and to create the one
   * that isn't there yet right next to them, rather than at the project root.
   */
  private fun fileLinks(
    project: Project,
    scanned: List<Pair<Path, Map<String, Map<String, String>>>>,
    name: String
  ): List<EnvironmentFileLink> =
    scanned.reversed()
      .filter { (_, environments) -> name in environments }
      .flatMap { (directory, _) ->
        EnvironmentFileKind.entries.map { kind ->
          val path = directory.resolve(kind.fileName)
          EnvironmentFileLink(
            kind = kind,
            path = path.toString(),
            presentablePath = EnvironmentFiles.presentablePath(project, path),
            exists = Files.isRegularFile(path)
          )
        }
      }

  private fun readEnvironmentDirectory(directory: Path): Map<String, Map<String, String>> {
    val base = readEnvironmentFile(directory.resolve(EnvironmentFileKind.BASE.fileName))
    val private = readEnvironmentFile(directory.resolve(EnvironmentFileKind.PRIVATE.fileName))
    return (base.keys + private.keys).associateWith { base[it].orEmpty() + private[it].orEmpty() }
  }

  private fun readEnvironmentFile(path: Path): Map<String, Map<String, String>> {
    if (!Files.isRegularFile(path)) return emptyMap()
    return try {
      @Suppress("UnstableApiUsage")
      Json.parseToJsonElement(EelFiles.readString(path)).jsonObject
        .filterKeys { it != SHARED_KEY }
        .mapValues { (_, value) ->
          value.jsonObject.mapNotNull { (k, v) ->
            (v as? JsonPrimitive)?.takeIf { it.isString }?.let { k to it.content }
          }.toMap()
        }
    } catch (e: Exception) {
      thisLogger().warn("Failed to parse $path as a run environments file", e)
      emptyMap()
    }
  }
}
