package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo

/**
 * Contributes run environments from some source (fixed list, `.env` files, deployment config, etc.).
 * Register an implementation via the
 * `io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.environmentSource`
 * extension point in your plugin.xml to add environments from another plugin.
 */
interface EnvironmentSource {
  /**
   * The environments this source currently contributes for [project].
   *
   * Called every time `EnvironmentAggregatorService` recomputes the aggregated list
   * (a `run.env.json` edit, a module added/removed, etc.),
   * so this should be cheap and free of side effects - do your own caching if reading it is expensive,
   * and re-trigger a recompute via a [BulkFileListener]/similar rather than polling.
   *
   * @param project the project to list environments for.
   * @return the environments this source contributes right now; empty if none.
   */
  fun listEnvironments(project: Project): List<EnvironmentInfo>

  companion object {
    val EP_NAME: ExtensionPointName<EnvironmentSource> =
      ExtensionPointName.create("io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.environmentSource")
  }
}
