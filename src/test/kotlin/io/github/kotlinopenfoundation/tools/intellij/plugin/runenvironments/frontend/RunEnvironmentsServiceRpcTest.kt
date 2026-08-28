package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileKind
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentFileLink
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

/**
 * Covers [RunEnvironmentsService]'s coordination logic against a [FakeEnvironmentsApi] instead of
 * the real backend - the real one does resolve fine in-process (it's the actual split-mode RPC
 * wiring, exercised end-to-end by [RunEnvironmentsRealBackendTest]), but asserting on it here would
 * mean depending on the real backend's file-system state and timing instead of controlling it
 * directly.
 *
 * Each test's timeout is a safety net, not an expected wait: the fake never actually suspends.
 */
class RunEnvironmentsServiceRpcTest : BasePlatformTestCase() {
  private val coroutineScope = CoroutineScope(SupervisorJob())
  private val api = FakeEnvironmentsApi()

  private fun service(): RunEnvironmentsService = RunEnvironmentsService(project, coroutineScope) { api }

  fun `test the environment list relays what the backend pushes`() = runBlocking {
    val service = service()

    withTimeout(5.seconds) {
      api.environments.value = listOf(EnvironmentInfo("dev"))
      assertEquals(listOf(EnvironmentInfo("dev")), service.environmentsFlow.first { it.isNotEmpty() })
    }
  }

  fun `test injection support relays what the backend pushes`() = runBlocking {
    val service = service()

    withTimeout(5.seconds) {
      api.injectionSupported.value = true
      assertTrue(service.injectionSupportedFlow.first { it })
    }
  }

  fun `test missing environment files relay what the backend pushes`() = runBlocking {
    val service = service()

    withTimeout(5.seconds) {
      api.missingFiles.value = listOf(PRIVATE)
      assertEquals(listOf(EnvironmentFileKind.PRIVATE), service.missingEnvironmentFilesFlow.first { it.isNotEmpty() })
    }
  }

  fun `test creating an environment file asks the backend`() = runBlocking {
    val service = service()

    service.createEnvironmentFile(PRIVATE)

    withTimeout(5.seconds) { assertEquals(EnvironmentFileKind.PRIVATE, api.createdFiles.receive()) }
  }

  fun `test opening an environment file asks the backend`() = runBlocking {
    val service = service()
    val link = EnvironmentFileLink(
      kind = EnvironmentFileKind.BASE,
      path = "/project/run.env.json",
      presentablePath = "run.env.json",
      exists = true
    )

    service.openEnvironmentFile(link)

    withTimeout(5.seconds) { assertEquals("/project/run.env.json", api.openedPaths.receive()) }
  }

  fun `test selecting an environment pushes the selection to the backend`() = runBlocking {
    val service = service()

    service.selectedEnvironment = "dev"

    // Constructing the service already queues one reconnect re-push of the (still null) selection
    // on this same channel, racing the explicit push below - drain until the real one shows up.
    withTimeout(5.seconds) {
      var pushed = api.selectedKeys.receive()
      while (pushed != "dev") {
        pushed = api.selectedKeys.receive()
      }
    }
  }

  fun `test a persisted selection is re-pushed once the backend connects`() = runBlocking {
    // Construction starts the flow eagerly, racing this loadState() call for whether the eager
    // collector reads the persisted selection or the still-default one - if it loses the race
    // there's no second push to recover on, so retry the whole attempt until it wins.
    withTimeout(5.seconds) {
      var pushed: String?
      do {
        val freshApi = FakeEnvironmentsApi()
        RunEnvironmentsService(project, coroutineScope) { freshApi }
          .loadState(RunEnvironmentsState(selectedEnvironment = "prod"))
        pushed = freshApi.selectedKeys.receive()
      } while (pushed != "prod")
    }
  }

  override fun tearDown() {
    try {
      coroutineScope.cancel()
    } finally {
      super.tearDown()
    }
  }
}
