package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api

import kotlinx.serialization.Serializable

/**
 * One environment file an environment could be defined in, offered in the UI as an edit link when
 * it [exists] and as a create link when it doesn't - a `run.env.json` without a
 * `run.private.env.json` next to it still gets an entry for the private file, pointing at the
 * directory the environment was detected in rather than at the project root.
 *
 * Crosses the frontend/backend RPC boundary, so it must stay serializable.
 */
@Serializable
data class EnvironmentFileLink(
  val kind: EnvironmentFileKind,
  /** Absolute path of the file - the identity the backend opens or creates. */
  val path: String,
  /** The path as shown to the user: relative to the project root when it's under it. */
  val presentablePath: String,
  val exists: Boolean
)
