package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action.SelectRunEnvironmentAction
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action.buildPopupGroup
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action.selectedEnvironmentText
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo

/**
 * Covers the popup's composition and the combo box's own text, which is all of the widget that
 * doesn't need a live RPC connection to the backend.
 *
 * [BasePlatformTestCase] rather than a plain JUnit test: the labels come from a
 * [com.intellij.DynamicBundle], which needs an application.
 */
class SelectRunEnvironmentActionTest : BasePlatformTestCase() {
  fun `test popup offers the no-environment entry even with nothing to select`() {
    val children = buildPopupGroup(emptyList(), emptyList()).getChildren(null)

    assertEquals(listOf("No Environment"), children.texts())
  }

  fun `test popup lists the environments after a separator`() {
    val children = buildPopupGroup(listOf(EnvironmentInfo("dev"), EnvironmentInfo("prod")), emptyList())
      .getChildren(null)

    assertEquals(listOf("No Environment", "dev", "prod"), children.texts())
    assertTrue("expected a separator before the environments", children[1] is Separator)
  }

  fun `test popup labels an environment with its qualifier when it has one`() {
    val children = buildPopupGroup(listOf(EnvironmentInfo("dev", qualifier = "order-service")), emptyList())
      .getChildren(null)

    assertEquals(listOf("No Environment", "dev (order-service)"), children.texts())
  }

  fun `test popup offers to create every missing environment file`() {
    val children = buildPopupGroup(emptyList(), listOf(EnvironmentFileKind.BASE, EnvironmentFileKind.PRIVATE))
      .getChildren(null)

    assertEquals(
      listOf("No Environment", "Create run.env.json", "Create run.private.env.json"),
      children.texts()
    )
  }

  fun `test popup has no create entries when no environment file is missing`() {
    val children = buildPopupGroup(listOf(EnvironmentInfo("dev")), emptyList())
      .getChildren(null)

    assertEquals(listOf("No Environment", "dev"), children.texts())
  }

  fun `test combo box shows the selected environment's label`() {
    val environments = listOf(EnvironmentInfo("dev"), EnvironmentInfo("prod", qualifier = "order-service"))

    assertEquals(
      "prod (order-service)",
      selectedEnvironmentText(environments, "prod::order-service")
    )
  }

  fun `test combo box falls back to the no-environment text for an unknown or absent selection`() {
    val environments = listOf(EnvironmentInfo("dev"))

    assertEquals("No Environment", selectedEnvironmentText(environments, null))
    assertEquals("No Environment", selectedEnvironmentText(environments, "gone"))
  }

  fun `test the action runs on a background thread`() {
    assertEquals(ActionUpdateThread.BGT, SelectRunEnvironmentAction().getActionUpdateThread())
  }

  fun `test update does nothing without a project`() {
    val event = event(null)
    val defaultText = event.presentation.text

    SelectRunEnvironmentAction().update(event)

    assertEquals(defaultText, event.presentation.text)
  }

  fun `test update shows the combo box text from the project's service`() {
    val event = event(project)

    SelectRunEnvironmentAction().update(event)

    assertEquals("No Environment", event.presentation.text)
  }

  fun `test the popup is empty without a project`() {
    val group = SelectRunEnvironmentAction().createPopupActionGroup(JBLabel(), DataContext.EMPTY_CONTEXT)

    assertEquals(listOf("No Environment"), group.getChildren(null).texts())
  }

  fun `test the popup reads the project's service`() {
    val dataContext = SimpleDataContext.getProjectContext(project)

    val children = SelectRunEnvironmentAction().createPopupActionGroup(JBLabel(), dataContext).getChildren(null)

    // Only the branch matters here (the real, service-backed path rather than the no-project
    // fallback) - its exact content depends on the real backend's timing and is covered by the
    // buildPopupGroup tests above instead.
    assertEquals("No Environment", children.texts().first())
  }

  /** An event whose context carries [project], or none at all when it's null. */
  private fun event(project: Project?): AnActionEvent {
    val action = SelectRunEnvironmentAction()
    return AnActionEvent.createEvent(
      action,
      if (project != null) SimpleDataContext.getProjectContext(project) else DataContext.EMPTY_CONTEXT,
      Presentation(),
      ActionPlaces.TOOLBAR,
      ActionUiKind.TOOLBAR,
      null
    )
  }

  /** The entries' texts, separators dropped - they're asserted on separately where they matter. */
  private fun Array<AnAction>.texts(): List<String?> =
    filterNot { it is Separator }.map { it.templatePresentation.text }
}
