package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api

import kotlinx.serialization.Serializable

/**
 * One of the two environment file kinds the JSON source reads. Crosses the frontend/backend RPC
 * boundary - the frontend offers to create the ones that don't exist yet - so it must stay
 * serializable.
 */
@Serializable
enum class EnvironmentFileKind(
  /** The file's name, identical in every directory the JSON source scans. */
  val fileName: String
) {
  /** Checked in, shared with the team. */
  BASE("run.env.json"),

  /** Local overrides and additions, meant to be ignored by VCS. */
  PRIVATE("run.private.env.json")
}
