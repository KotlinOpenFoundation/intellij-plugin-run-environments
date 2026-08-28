package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action.SelectEnvironmentItemAction
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileLink

/** Covers the per-environment submenu: when it appears at all, and what it offers. */
class SelectEnvironmentItemActionTest : BasePlatformTestCase() {
  fun `test an environment with files is both performable and a submenu`() {
    val action = SelectEnvironmentItemAction("dev", "dev", listOf(link("run.env.json", exists = true)))

    assertTrue("clicking the label should still select the environment", action.templatePresentation.isPerformGroup)
    assertTrue("the files should be reachable through a submenu", action.templatePresentation.isPopupGroup)
  }

  fun `test an entry with no files stays a plain item`() {
    val action = SelectEnvironmentItemAction(null, "No Environment")

    assertTrue(action.templatePresentation.isPerformGroup)
    assertFalse("an entry with nothing to show shouldn't get an arrow", action.templatePresentation.isPopupGroup)
    assertEmpty(action.getChildren(null))
  }

  fun `test the submenu edits an existing file and creates a missing one`() {
    val action = SelectEnvironmentItemAction(
      "dev",
      "dev",
      listOf(
        link(".run/run.env.json", exists = true),
        link(".run/run.private.env.json", exists = false, kind = EnvironmentFileKind.PRIVATE)
      )
    )

    assertEquals(
      listOf("Edit .run/run.env.json", "Create .run/run.private.env.json"),
      action.getChildren(null).map { it.templatePresentation.text }
    )
  }

  fun `test the action runs on a background thread`() {
    assertEquals(
      ActionUpdateThread.BGT,
      SelectEnvironmentItemAction(null, "No Environment").getActionUpdateThread()
    )
  }

  fun `test selecting an entry does nothing without a project`() {
    val action = SelectEnvironmentItemAction("dev", "dev")

    action.actionPerformed(event(action, null))
  }

  fun `test selecting an entry updates the service's selection`() {
    val action = SelectEnvironmentItemAction("dev", "dev")

    action.actionPerformed(event(action, project))

    assertEquals("dev", project.runEnvironments.selectedEnvironment)
  }

  /** An event whose context carries [project], or none at all when it's null. */
  private fun event(action: AnAction, project: Project?): AnActionEvent =
    AnActionEvent.createEvent(
      action,
      if (project != null) SimpleDataContext.getProjectContext(project) else DataContext.EMPTY_CONTEXT,
      Presentation(),
      ActionPlaces.TOOLBAR,
      ActionUiKind.TOOLBAR,
      null
    )

  private fun link(
    presentablePath: String,
    exists: Boolean,
    kind: EnvironmentFileKind = EnvironmentFileKind.BASE
  ): EnvironmentFileLink =
    EnvironmentFileLink(
      kind = kind,
      path = "/project/$presentablePath",
      presentablePath = presentablePath,
      exists = exists
    )
}
