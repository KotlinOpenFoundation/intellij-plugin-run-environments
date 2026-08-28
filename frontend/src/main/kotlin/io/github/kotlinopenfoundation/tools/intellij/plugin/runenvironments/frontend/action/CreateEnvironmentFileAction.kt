package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.runEnvironments
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind

/**
 * Combo box entry offered for an environment file the project doesn't have yet - creating it at
 * the project root and opening it, the way the HTTP Client offers to add an environment file when
 * there's none to select from.
 */
internal class CreateEnvironmentFileAction(
  private val kind: EnvironmentFileKind,
  text: String
) : AnAction(text) {
  override fun getActionUpdateThread(): ActionUpdateThread = BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    project.runEnvironments.createEnvironmentFile(kind)
  }
}
