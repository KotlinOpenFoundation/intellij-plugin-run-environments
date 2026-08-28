package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.RunEnvironmentsService
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.runEnvironments

/**
 * Groups [RunWithLabelAction] and [SelectRunEnvironmentAction] so they appear and disappear
 * together as a single unit - hidden whenever the toolbar's currently selected run configuration
 * doesn't support environment injection (Shell Script, Docker, npm, ...), rather than each of the
 * two children independently checking [RunEnvironmentsService.injectionSupported].
 */
class RunEnvironmentsActionGroup : DefaultActionGroup() {
  override fun getActionUpdateThread(): ActionUpdateThread = BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.project?.runEnvironments?.injectionSupported == true
  }
}
