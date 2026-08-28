package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.RunEnvironmentsFrontendBundle
import javax.swing.JComponent

/**
 * Static "Run with:" label rendered just to the left of [SelectRunEnvironmentAction]'s combo box.
 *
 * Visibility isn't handled here - [RunEnvironmentsActionGroup] hides this together with
 * [SelectRunEnvironmentAction] as a single unit.
 */
class RunWithLabelAction : AnAction(), CustomComponentAction {
  override fun getActionUpdateThread(): ActionUpdateThread = BGT

  override fun actionPerformed(e: AnActionEvent) = Unit

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent =
    JBLabel(RunEnvironmentsFrontendBundle.message("label.RunWith.text")).apply {
      border = JBUI.Borders.emptyRight(4)
    }
}
