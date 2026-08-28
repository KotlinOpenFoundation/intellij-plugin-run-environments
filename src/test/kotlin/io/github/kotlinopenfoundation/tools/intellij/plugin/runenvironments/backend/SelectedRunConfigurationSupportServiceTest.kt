package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.configurations.UnknownConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentInjectionSupport

class SelectedRunConfigurationSupportServiceTest : BasePlatformTestCase() {
  private val supportsEverything = object : EnvironmentInjectionSupport {
    override fun supports(configuration: RunConfiguration) = true
  }

  // The light project's RunManager persists across every test in this suite, not just this class's
  // own methods - leaving a configuration selected here would poison whichever test elsewhere is
  // the first to construct SelectedRunConfigurationSupportService afterward.
  override fun tearDown() {
    try {
      RunManager.getInstance(project).selectedConfiguration = null
    } finally {
      super.tearDown()
    }
  }

  fun `test reports unsupported when nothing is selected`() {
    // The light project's RunManager persists across test methods - a prior test may have left a
    // configuration selected.
    RunManager.getInstance(project).selectedConfiguration = null

    val service = SelectedRunConfigurationSupportService(project)
    try {
      assertFalse(service.isSupportedFlow.value)
    } finally {
      Disposer.dispose(service)
    }
  }

  fun `test refreshes when the selected configuration's settings change`() {
    val runManager = RunManager.getInstance(project)
    val settings = runManager.createConfiguration("test", UnknownConfigurationType.getInstance())
    runManager.addConfiguration(settings)
    runManager.selectedConfiguration = settings

    val service = SelectedRunConfigurationSupportService(project)
    try {
      assertFalse(service.isSupportedFlow.value)

      EnvironmentInjectionSupport.EP_NAME.point.registerExtension(supportsEverything, testRootDisposable)
      project.messageBus.syncPublisher(RunManagerListener.TOPIC).runConfigurationChanged(settings)

      assertTrue(service.isSupportedFlow.value)
    } finally {
      Disposer.dispose(service)
    }
  }

  fun `test refreshes when the selection itself changes`() {
    // The light project's RunManager persists across test methods - a prior test may have left a
    // configuration selected.
    RunManager.getInstance(project).selectedConfiguration = null
    EnvironmentInjectionSupport.EP_NAME.point.registerExtension(supportsEverything, testRootDisposable)

    val service = SelectedRunConfigurationSupportService(project)
    try {
      assertFalse(service.isSupportedFlow.value)

      val runManager = RunManager.getInstance(project)
      val settings = runManager.createConfiguration("test", UnknownConfigurationType.getInstance())
      runManager.addConfiguration(settings)
      runManager.selectedConfiguration = settings
      project.messageBus.syncPublisher(RunManagerListener.TOPIC).runConfigurationSelected(settings)

      assertTrue(service.isSupportedFlow.value)
    } finally {
      Disposer.dispose(service)
    }
  }
}
