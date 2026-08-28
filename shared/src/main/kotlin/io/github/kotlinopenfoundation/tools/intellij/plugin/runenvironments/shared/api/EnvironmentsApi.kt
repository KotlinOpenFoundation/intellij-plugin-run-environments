package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api

import com.intellij.platform.project.ProjectId
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import kotlinx.coroutines.flow.Flow

/** Frontend-callable entry point for the aggregated, project-scoped environment list. */
@Rpc
interface EnvironmentsApi : RemoteApi<Unit> {
  /**
   * Pushes a new list every time the environment set changes - no polling needed.
   *
   * @param projectId the project to list environments for.
   * @return a flow that immediately emits the current environments, then a new list every time
   *   the aggregated set changes (a `run.env.json` edit, a module added/removed, ...).
   */
  suspend fun getEnvironments(projectId: ProjectId): Flow<List<EnvironmentInfo>>

  /**
   * Tells the backend which environment the frontend has selected, so run configuration
   * launches (which execute backend-side) can inject that environment's [EnvironmentInfo.variables].
   *
   * @param projectId the project the selection belongs to.
   * @param key an [EnvironmentInfo.key], or `null` for "no environment selected".
   */
  suspend fun setSelectedEnvironment(projectId: ProjectId, key: String?)

  /**
   * Pushes a new value whenever the toolbar's currently selected run configuration (or its own
   * settings) changes, reflecting whether any registered injection mechanism actually supports it.
   *
   * @param projectId the project whose toolbar selection to track.
   * @return a flow that immediately emits the current support state, then a new value every time
   *   the toolbar's selected run configuration (or its settings) changes.
   */
  suspend fun isInjectionSupported(projectId: ProjectId): Flow<Boolean>

  /**
   * Pushes the environment file kinds the project doesn't have anywhere yet, so the frontend can
   * offer to create them instead of leaving a project with no environment file at a dead end.
   *
   * @param projectId the project whose environment files to track.
   * @return a flow that immediately emits the currently missing kinds, then a new list every time
   *   one of them is created, deleted or renamed.
   */
  suspend fun getMissingEnvironmentFiles(projectId: ProjectId): Flow<List<EnvironmentFileKind>>

  /**
   * Creates [kind]'s file at the project root and opens it in the editor. A no-op beyond opening
   * it if the file already exists.
   *
   * @param projectId the project to create the file in.
   * @param kind the environment file to create.
   */
  suspend fun createEnvironmentFile(projectId: ProjectId, kind: EnvironmentFileKind)

  /**
   * Opens the environment file at [path], creating it first if it doesn't exist yet - the call
   * behind an [EnvironmentInfo.files] link, which points at the directory the environment was
   * detected in rather than at the project root.
   *
   * Ignored for a [path] that isn't an environment file in one of the directories the backend
   * scans.
   *
   * @param projectId the project the file belongs to.
   * @param path an [EnvironmentFileLink.path].
   */
  suspend fun openEnvironmentFile(projectId: ProjectId, path: String)
}
