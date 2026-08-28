package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentSource
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Merges [EnvironmentInfo] contributions from every registered [EnvironmentSource], and keeps
 * that list live: [environments] pushes a new value whenever a source might have changed,
 * instead of making callers poll.
 *
 * This is the seam the frontend calls through over RPC instead of hardcoding a list itself, so
 * third-party plugins can add sources without the frontend knowing about them.
 *
 * A [EnvironmentInfo.name] contributed by only one source is exposed as-is, with its qualifier
 * cleared - disambiguation only kicks in once two sources genuinely disagree on the same name.
 */
@Service(PROJECT)
class EnvironmentAggregatorService(
  private val project: Project
) : Disposable {
  private val _environments = MutableStateFlow(computeEnvironments())
  val environments: StateFlow<List<EnvironmentInfo>> = _environments.asStateFlow()

  init {
    EnvironmentFiles.subscribeToChanges(project, parentDisposable = this) { refresh() }
  }

  private fun refresh() {
    _environments.value = computeEnvironments()
  }

  private fun computeEnvironments(): List<EnvironmentInfo> =
    mergeEnvironmentContributions(
      EnvironmentSource.EP_NAME.extensionList.flatMap { it.listEnvironments(project) },
      rootQualifier = "<root>"
    )

  override fun dispose() = Unit

  companion object {
    /**
     * Collapses [contributions] to one [EnvironmentInfo] per distinct name, keeping each source's
     * [EnvironmentInfo.variables] intact:
     * - a name contributed by only one source is exposed as-is minus its qualifier - unambiguous, so
     *   no disambiguating label is needed;
     * - a name two or more sources disagree on keeps each contribution's qualifier, falling back to
     *   [rootQualifier] for one with none (e.g. the project root's own `run.env.json`, which has no
     *   natural qualifier of its own) so it doesn't read as the unqualified, "canonical" one merely by
     *   virtue of being first.
     */
    fun mergeEnvironmentContributions(contributions: List<EnvironmentInfo>, rootQualifier: String): List<EnvironmentInfo> =
      contributions.distinct()
        .groupBy { it.name }
        .flatMap { (_, group) ->
          when {
            group.size == 1 -> listOf(group.single().copy(qualifier = null))
            else -> group.map { if (it.qualifier == null) it.copy(qualifier = rootQualifier) else it }
          }
        }
  }
}
