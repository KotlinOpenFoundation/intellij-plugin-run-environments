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
import com.intellij.testFramework.replaceService
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action.CreateEnvironmentFileAction
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.action.OpenEnvironmentFileAction
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * Covers the two file-opening combo box entries against the real, service-backed
 * [io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend.runEnvironments]
 * extension, replacing that project's [RunEnvironmentsService] with one backed by a
 * [FakeEnvironmentsApi] for the duration of each test, so the call each action fires is
 * deterministic and immediately observable instead of depending on the real backend's timing.
 */
class EnvironmentFileActionsTest : BasePlatformTestCase() {
  private val coroutineScope = CoroutineScope(SupervisorJob())
  private val api = FakeEnvironmentsApi()

  override fun setUp() {
    super.setUp()
    val service = RunEnvironmentsService(project, coroutineScope) { api }
    project.replaceService(RunEnvironmentsService::class.java, service, testRootDisposable)
  }

  override fun tearDown() {
    try {
      coroutineScope.cancel()
    } finally {
      super.tearDown()
    }
  }

  fun `test create action runs on a background thread`() {
    assertEquals(
      ActionUpdateThread.BGT,
      CreateEnvironmentFileAction(EnvironmentFileKind.BASE, "Create").getActionUpdateThread()
    )
  }

  fun `test create action does nothing without a project`() {
    val action = CreateEnvironmentFileAction(EnvironmentFileKind.BASE, "Create")

    action.actionPerformed(event(action, null))
  }

  fun `test create action asks the service to create the file`() = runBlocking {
    val action = CreateEnvironmentFileAction(EnvironmentFileKind.PRIVATE, "Create")

    action.actionPerformed(event(action, project))

    withTimeout(5.seconds) { assertEquals(EnvironmentFileKind.PRIVATE, api.createdFiles.receive()) }
  }

  fun `test open action runs on a background thread`() {
    assertEquals(ActionUpdateThread.BGT, OpenEnvironmentFileAction(link(), "Open").getActionUpdateThread())
  }

  fun `test open action does nothing without a project`() {
    val action = OpenEnvironmentFileAction(link(), "Open")

    action.actionPerformed(event(action, null))
  }

  fun `test open action asks the service to open the file`() = runBlocking {
    val action = OpenEnvironmentFileAction(link(), "Open")

    action.actionPerformed(event(action, project))

    withTimeout(5.seconds) { assertEquals("/project/run.env.json", api.openedPaths.receive()) }
  }

  private fun link(): EnvironmentFileLink =
    EnvironmentFileLink(
      kind = EnvironmentFileKind.BASE,
      path = "/project/run.env.json",
      presentablePath = "run.env.json",
      exists = true
    )

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
}
