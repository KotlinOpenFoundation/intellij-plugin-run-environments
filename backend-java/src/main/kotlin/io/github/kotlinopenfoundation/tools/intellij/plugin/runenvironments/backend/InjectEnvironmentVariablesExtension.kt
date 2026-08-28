package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentInjectionSupport
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo

/**
 * Injects the currently selected run environment's [EnvironmentInfo.variables] into the process
 * environment of every launched run configuration whose execution state extends
 * `JavaCommandLineState` - and also declares [supports] for that same condition, so the frontend
 * knows to offer the environment selector for these configurations.
 *
 * Overrides both hooks the platform offers for injection: [updateJavaParameters] for Java-
 * parameter-based configurations (Application, JUnit, ...), and [patchCommandLine] for everything
 * else that launches through a plain [GeneralCommandLine]. Both end up touching the same eventual
 * process environment, so covering both is redundant for Java configurations but harmless - the
 * values are identical.
 *
 * [JavaRunConfigurationBase] covers Application/JUnit/TestNG/Kotlin-JVM/JAR-Application directly.
 * Maven's *default* (non-wrapper) run configuration is a special case: `MavenRunConfiguration`
 * itself does not extend [JavaRunConfigurationBase], but its default execution state does extend
 * `JavaCommandLineState` - matched by class name here to avoid a hard dependency on the Maven
 * plugin just for this check. This over-claims for the one case where "Use Maven wrapper" is
 * enabled on that same configuration (a different execution path this plugin doesn't support) -
 * a known, narrow false positive, not a functional regression since nothing else claims it either.
 */
class InjectEnvironmentVariablesExtension : RunConfigurationExtension(), EnvironmentInjectionSupport {
  override fun supports(configuration: RunConfiguration): Boolean =
    configuration is JavaRunConfigurationBase ||
      configuration.javaClass.name == "org.jetbrains.idea.maven.execution.MavenRunConfiguration"

  override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean = supports(configuration)

  override fun <T : RunConfigurationBase<*>> updateJavaParameters(
    configuration: T,
    javaParameters: JavaParameters,
    runnerSettings: RunnerSettings?
  ) {
    javaParameters.env.putAll(configuration.project.selectedEnvironmentVariables())
  }

  override fun patchCommandLine(
    configuration: RunConfigurationBase<*>,
    runnerSettings: RunnerSettings?,
    cmdLine: GeneralCommandLine,
    runnerId: String
  ) {
    cmdLine.environment.putAll(configuration.project.selectedEnvironmentVariables())
  }
}
