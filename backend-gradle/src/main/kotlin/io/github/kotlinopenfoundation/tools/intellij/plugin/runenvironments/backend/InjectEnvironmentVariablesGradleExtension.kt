package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentInjectionSupport
import org.gradle.tooling.LongRunningOperation
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionContext
import org.jetbrains.plugins.gradle.service.project.GradleExecutionHelperExtension
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Injects the currently selected run environment's variables into every Gradle task IntelliJ
 * executes for this project - both an explicit "Gradle" run configuration and an Application/
 * JUnit/... configuration with "Build and run using: Gradle" delegation turned on. Both execute
 * through this same Gradle Tooling API path and never reach `InjectEnvironmentVariablesExtension`,
 * which only fires for a native (non-delegated) launch.
 *
 * Also declares [supports] for that same condition (a Gradle-backed [ExternalSystemRunConfiguration]),
 * so the frontend knows to offer the environment selector for these configurations.
 *
 * [GradleExecutionSettings.addEnvironmentVariable] is additive - it layers onto whatever
 * environment the Gradle daemon process otherwise inherits, rather than replacing it wholesale
 * (as calling the Tooling API's [LongRunningOperation.setEnvironmentVariables] directly would).
 */
class InjectEnvironmentVariablesGradleExtension : GradleExecutionHelperExtension, EnvironmentInjectionSupport {
  override fun supports(configuration: RunConfiguration): Boolean =
    configuration is ExternalSystemRunConfiguration && configuration.settings.externalSystemId == GradleConstants.SYSTEM_ID

  override fun configureSettings(settings: GradleExecutionSettings, context: GradleExecutionContext) {
    for ((key, value) in context.project.selectedEnvironmentVariables()) {
      settings.addEnvironmentVariable(key, value)
    }
  }
}
