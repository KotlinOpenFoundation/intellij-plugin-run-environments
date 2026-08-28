package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.execution.process.NopProcessHandler
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import javax.swing.JPanel

// RunContentDescriptor's constructor needs a live Application to create its coroutine scope -
// BasePlatformTestCase's light fixture provides one, a plain JUnit4 test can't.
class RunDashboardEnvironmentCustomizerTest : BasePlatformTestCase() {
  fun `test returns null when the descriptor is null`() {
    assertNull(launchEnvironmentLabel(null))
  }

  fun `test returns null when the descriptor's process was never tagged`() {
    val descriptor = RunContentDescriptor(null, NopProcessHandler(), JPanel(), "test")

    assertNull(launchEnvironmentLabel(descriptor))
  }

  fun `test returns the label RunEnvironmentLaunchTagger stamped on the process handler`() {
    val handler = NopProcessHandler()
    handler.putUserData(RUN_ENVIRONMENT_LAUNCH_TAG, "dev")
    val descriptor = RunContentDescriptor(null, handler, JPanel(), "test")

    assertEquals("dev", launchEnvironmentLabel(descriptor))
  }
}
