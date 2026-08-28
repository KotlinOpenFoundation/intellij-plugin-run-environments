package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentInjectionSupport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the project's currently *selected* run configuration - the one shown in the main
 * toolbar's run widget - is one some registered [EnvironmentInjectionSupport] can actually inject
 * variables into.
 *
 * Refreshed both when the selection itself changes and when the selected configuration's own
 * settings are edited (e.g. toggling "Build and run using: Gradle" doesn't change *which*
 * configuration is selected, but does change whether injection applies to it).
 */
@Service(PROJECT)
class SelectedRunConfigurationSupportService(private val project: Project) : Disposable {
  private val _isSupportedFlow = MutableStateFlow(computeIsSupported())
  val isSupportedFlow: StateFlow<Boolean> = _isSupportedFlow.asStateFlow()

  init {
    project.messageBus.connect(this).subscribe(RunManagerListener.TOPIC, object : RunManagerListener {
      override fun runConfigurationSelected(settings: RunnerAndConfigurationSettings?) = refresh()
      override fun runConfigurationChanged(settings: RunnerAndConfigurationSettings) = refresh()
    })
  }

  private fun refresh() {
    _isSupportedFlow.value = computeIsSupported()
  }

  private fun computeIsSupported(): Boolean = EnvironmentInjectionSupport.isInjectionSupported(
    RunManager.getInstance(project).selectedConfiguration?.configuration,
    EnvironmentInjectionSupport.EP_NAME.extensionList
  )

  override fun dispose() = Unit
}
