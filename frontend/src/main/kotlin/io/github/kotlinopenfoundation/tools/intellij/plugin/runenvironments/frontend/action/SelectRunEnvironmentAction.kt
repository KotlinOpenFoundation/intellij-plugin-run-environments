package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.RunEnvironmentsFrontendBundle
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.runEnvironments
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo
import javax.swing.JComponent

/**
 * Toolbar widget that lets the user pick the active run environment.
 *
 * Mirrors the HTTP Client environment selector: a combo box showing the current selection that
 * pops up a flat list of environments to switch between, each with its own submenu of environment
 * file links, and entries to create an environment file the project doesn't have yet.
 *
 * Visibility isn't handled here - [RunEnvironmentsActionGroup] hides this together with
 * [RunWithLabelAction] as a single unit.
 */
class SelectRunEnvironmentAction : ComboBoxAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = BGT

  override fun update(e: AnActionEvent) {
    val service = e.project?.runEnvironments ?: return
    e.presentation.text = selectedEnvironmentText(service.environments, service.selectedEnvironment)
  }

  public override fun createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup {
    val service = dataContext.getData(CommonDataKeys.PROJECT)?.runEnvironments
      ?: return buildPopupGroup(emptyList(), emptyList())
    return buildPopupGroup(service.environments, service.missingEnvironmentFiles)
  }
}

/** The combo box's own text: the selected environment's label, or the "no environment" one. */
fun selectedEnvironmentText(environments: List<EnvironmentInfo>, selectedKey: String?): String =
  environments.find { it.key == selectedKey }?.label
    ?: RunEnvironmentsFrontendBundle.message("combo.NoEnvironment.text")

/**
 * The popup's contents: the "no environment" entry, then [environments], then - separated
 * from them - an entry per [missingFiles] kind offering to create that file at the project
 * root, so a project with no environment file at all isn't a dead end.
 */
fun buildPopupGroup(
  environments: List<EnvironmentInfo>,
  missingFiles: List<EnvironmentFileKind>
): DefaultActionGroup {
  val group = DefaultActionGroup()
  group.add(SelectEnvironmentItemAction(null, RunEnvironmentsFrontendBundle.message("combo.NoEnvironment.text")))

  group.addSeparator()
  for (environment in environments) {
    group.add(SelectEnvironmentItemAction(environment.key, environment.label, environment.files))
  }

  if (missingFiles.isNotEmpty()) {
    group.addSeparator()
    for (kind in missingFiles) {
      group.add(
        CreateEnvironmentFileAction(
          kind,
          RunEnvironmentsFrontendBundle.message("action.CreateEnvironmentFile.text", kind.fileName)
        )
      )
    }
  }
  return group
}
