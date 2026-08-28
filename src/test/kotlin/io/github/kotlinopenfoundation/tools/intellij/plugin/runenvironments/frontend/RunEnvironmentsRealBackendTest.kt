package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.frontend

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Exercises the real, end-to-end path that [RunEnvironmentsServiceRpcTest] replaces with a fake:
 * the actual split-mode RPC resolution, the backend's
 * [io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.EnvironmentsApiProvider]
 * registration, and the real backend services reading the project's own files. It resolves fine in
 * a plain [BasePlatformTestCase] - no split-mode process pair is actually needed for this.
 *
 * Polls rather than asserting immediately: the real backend computes its first value
 * asynchronously, so there's no guaranteed-instant point at which to read it.
 */
class RunEnvironmentsRealBackendTest : BasePlatformTestCase() {
  fun `test the real backend reports no missing environment files once both exist`() = runBlocking {
    myFixture.addFileToProject("run.env.json", "{}")
    myFixture.addFileToProject("run.private.env.json", "{}")

    withTimeout(10.seconds) {
      while (project.runEnvironments.missingEnvironmentFiles.isNotEmpty()) {
        delay(50.milliseconds)
      }
    }
  }
}
