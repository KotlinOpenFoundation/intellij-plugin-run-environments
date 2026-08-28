package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api

import kotlinx.serialization.Serializable

/**
 * A single run environment contributed by some source (fixed list, `.env` file, backend config
 * discovery, ...). Crosses the frontend/backend RPC boundary, so it must stay serializable.
 *
 * [qualifier] disambiguates environments that share a [name] but come from different sources
 * (e.g. two modules both defining "dev") - it should stay `null`, and unset, whenever the name
 * alone is already unambiguous, since the UI only shows it when needed.
 */
@Serializable
data class EnvironmentInfo(
  /** The environment's display name, e.g. `"dev"` or `"prod"`. */
  val name: String,
  val qualifier: String? = null,
  /** The environment variables to inject into a run configuration's process when this environment is selected. */
  val variables: Map<String, String> = emptyMap(),
  /**
   * The files this environment could be defined in, for the edit/create links the UI offers next
   * to it - empty for a source that isn't file-backed.
   */
  val files: List<EnvironmentFileLink> = emptyList()
) {
  /** Stable identity for persisting a selection, distinct even when [name] collides. */
  val key: String get() = if (qualifier != null) "$name::$qualifier" else name

  /** Display label for the UI - the toolbar combo box, the Services tree, ... */
  val label: String get() = if (qualifier != null) "$name ($qualifier)" else name
}
