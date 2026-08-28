package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo

/**
 * The [EnvironmentInfo.variables] of whichever [environments] entry's [EnvironmentInfo.key] matches [selectedKey],
 * or empty if [selectedKey] is `null` (nothing selected) or matches none
 * (stale selection - the environment was renamed/removed since).
 */
fun selectedEnvironmentVariables(environments: List<EnvironmentInfo>, selectedKey: String?): Map<String, String> =
  selectedKey?.let { key -> environments.find { it.key == key }?.variables } ?: emptyMap()

/**
 * The project's currently selected run environment's [EnvironmentInfo.variables].
 */
fun Project.selectedEnvironmentVariables(): Map<String, String> = selectedEnvironmentVariables(
  service<EnvironmentAggregatorService>().environments.value,
  service<SelectedEnvironmentService>().selectedKey
)

/**
 * The [EnvironmentInfo.label] of whichever [environments] entry's [EnvironmentInfo.key] matches [selectedKey],
 * or `null` if [selectedKey] is `null` (nothing selected) or matches none (stale selection).
 */
fun selectedEnvironmentLabel(environments: List<EnvironmentInfo>, selectedKey: String?): String? =
  selectedKey?.let { key -> environments.find { it.key == key }?.label }

/**
 * The project's currently selected run environment's [EnvironmentInfo.label], `null` if none is selected.
 */
fun Project.selectedEnvironmentLabel(): String? = selectedEnvironmentLabel(
  service<EnvironmentAggregatorService>().environments.value,
  service<SelectedEnvironmentService>().selectedKey
)
