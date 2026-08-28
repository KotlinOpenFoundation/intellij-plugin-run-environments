package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend

import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.UnknownConfigurationType
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentInjectionSupport
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action.RunEnvironmentsActionGroup
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action.RunWithLabelAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/** Covers the toolbar pieces around the combo box itself. */
class RunEnvironmentsToolbarTest : BasePlatformTestCase() {
  // The light project's RunManager persists across every test in the suite - a prior test may
  // have left a configuration selected, and leaving ours selected would poison whichever test
  // elsewhere is the next to construct SelectedRunConfigurationSupportService.
  override fun tearDown() {
    try {
      RunManager.getInstance(project).selectedConfiguration = null
    } finally {
      super.tearDown()
    }
  }

  fun `test the group is hidden without a project`() {
    val group = RunEnvironmentsActionGroup()
    val event = event(group)

    group.update(event)

    assertFalse(event.presentation.isEnabledAndVisible)
  }

  fun `test the group is hidden when injection is not supported`() {
    val group = RunEnvironmentsActionGroup()
    val event = event(group, project)

    group.update(event)

    assertFalse(event.presentation.isEnabledAndVisible)
  }

  fun `test the group is shown when injection is supported`() = runBlocking {
    RunManager.getInstanceAsync(project).selectedConfiguration = null
    val runManager = RunManager.getInstanceAsync(project)
    val settings = runManager.createConfiguration("test", UnknownConfigurationType.getInstance())
    runManager.addConfiguration(settings)
    runManager.selectedConfiguration = settings

    EnvironmentInjectionSupport.EP_NAME.point.registerExtension(
      object : EnvironmentInjectionSupport {
        override fun supports(configuration: RunConfiguration) = true
      },
      testRootDisposable
    )
    project.messageBus.syncPublisher(RunManagerListener.TOPIC).runConfigurationChanged(settings)

    withTimeout(5.seconds) {
      project.runEnvironments.injectionSupportedFlow.first { it }
    }

    val group = RunEnvironmentsActionGroup()
    val event = event(group, project)
    group.update(event)

    assertTrue(event.presentation.isEnabledAndVisible)
  }

  fun `test the group and label run on a background thread`() {
    assertEquals(ActionUpdateThread.BGT, RunEnvironmentsActionGroup().getActionUpdateThread())
    assertEquals(ActionUpdateThread.BGT, RunWithLabelAction().getActionUpdateThread())
  }

  fun `test the label action performs no operation`() {
    val action = RunWithLabelAction()

    action.actionPerformed(event(action))
  }

  fun `test the label renders the run with text`() {
    val component = RunWithLabelAction().createCustomComponent(Presentation(), ActionPlaces.TOOLBAR)

    assertEquals("Run with:", (component as JBLabel).text)
  }

  /** An event with no project in its context - enough for the "nothing to act on" paths. */
  private fun event(action: AnAction, project: Project? = null): AnActionEvent =
    AnActionEvent.createEvent(
      action,
      if (project != null) SimpleDataContext.getProjectContext(project) else DataContext.EMPTY_CONTEXT,
      Presentation(),
      ActionPlaces.TOOLBAR,
      ActionUiKind.TOOLBAR,
      null
    )
}
