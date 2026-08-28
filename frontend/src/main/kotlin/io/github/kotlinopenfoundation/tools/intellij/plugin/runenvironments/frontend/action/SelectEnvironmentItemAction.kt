package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.RunEnvironmentsFrontendBundle
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.runEnvironments
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileLink

/**
 * One entry of the environment combo box. Clicking the label selects the environment; the arrow on
 * its right opens a submenu with the files it's defined in - edit links for the ones that exist,
 * create links for the ones that don't, all in the directory the environment was detected in.
 *
 * It's both a performable action and a popup group: [com.intellij.openapi.actionSystem.Presentation.setPerformGroup]
 * keeps the click on the label doing the selection instead of only expanding the submenu. An entry
 * with no files behind it (the "no environment" one, an environment from a non-file source) stays
 * a plain item, without an arrow.
 */
class SelectEnvironmentItemAction(
  private val environmentKey: String?,
  text: String,
  private val files: List<EnvironmentFileLink> = emptyList()
) : ActionGroup(text, null, null) {
  init {
    templatePresentation.isPopupGroup = files.isNotEmpty()
    templatePresentation.isPerformGroup = true
  }

  override fun getActionUpdateThread(): ActionUpdateThread = BGT

  override fun getChildren(e: AnActionEvent?): Array<AnAction> =
    files.map { link ->
      val messageKey = if (link.exists) "action.EditEnvironmentFile.text" else "action.CreateEnvironmentFile.text"
      OpenEnvironmentFileAction(link, RunEnvironmentsFrontendBundle.message(messageKey, link.presentablePath))
    }.toTypedArray()

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    project.runEnvironments.selectedEnvironment = environmentKey
  }
}
