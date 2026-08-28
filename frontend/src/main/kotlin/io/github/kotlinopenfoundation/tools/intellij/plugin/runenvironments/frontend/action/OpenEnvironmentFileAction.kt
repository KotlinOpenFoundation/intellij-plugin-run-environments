package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.runEnvironments
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileLink

/**
 * Submenu entry of [SelectEnvironmentItemAction] that opens one of an environment's files,
 * creating it first when it's a link to a file that doesn't exist yet.
 */
internal class OpenEnvironmentFileAction(
  private val link: EnvironmentFileLink,
  text: String
) : AnAction(text) {
  override fun getActionUpdateThread(): ActionUpdateThread = BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    project.runEnvironments.openEnvironmentFile(link)
  }
}
