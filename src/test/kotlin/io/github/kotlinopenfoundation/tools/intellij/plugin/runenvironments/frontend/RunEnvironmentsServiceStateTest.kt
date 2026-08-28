package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Covers the part of [RunEnvironmentsService] that survives without a backend: the selection it
 * persists across IDE restarts. The environment and injection flows need a live RPC connection,
 * so they're left to the backend's own tests.
 *
 * The service is built by hand on a scope this test owns, rather than taken from the project, so
 * the flows it starts eagerly are cancelled again at [tearDown] instead of retrying for the rest
 * of the run.
 */
class RunEnvironmentsServiceStateTest : BasePlatformTestCase() {
  private val coroutineScope = CoroutineScope(SupervisorJob())

  fun `test the selected environment is persisted`() {
    val service = RunEnvironmentsService(project, coroutineScope)

    service.selectedEnvironment = "dev::order-service"

    assertEquals("dev::order-service", service.state.selectedEnvironment)
  }

  fun `test a persisted selection is restored`() {
    val service = RunEnvironmentsService(project, coroutineScope)

    service.loadState(RunEnvironmentsState(selectedEnvironment = "prod"))

    assertEquals("prod", service.selectedEnvironment)
  }

  fun `test no environment is selected by default`() {
    val service = RunEnvironmentsService(project, coroutineScope)

    assertNull(service.selectedEnvironment)
  }

  fun `test the project extension resolves the registered service`() {
    assertSame(project.service<RunEnvironmentsService>(), project.runEnvironments)
  }

  override fun tearDown() {
    try {
      coroutineScope.cancel()
    } finally {
      super.tearDown()
    }
  }
}
