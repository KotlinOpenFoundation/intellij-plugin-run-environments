package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.dashboard.RunDashboardCustomizationBuilder
import com.intellij.execution.dashboard.RunDashboardCustomizer
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.ui.SimpleTextAttributes
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentInjectionSupport

/**
 * Appends the environment a Services-tree node's process was launched with - e.g. `" [dev]"` - next
 * to the node's own text, reading the label [RunEnvironmentLaunchTagger] stamped onto its
 * [RunContentDescriptor]'s process handler at launch time.
 *
 * Uses the [RunDashboardCustomizationBuilder] overload rather than the legacy `PresentationData`
 * one - the only one whose result actually crosses the frontend/backend split-mode RPC boundary
 * the platform sets up for the Services tree, since this customizer necessarily runs backend-side
 * (that's where [RunEnvironmentLaunchTagger] and the launched process itself live).
 *
 * Registered with `order="first"` and always returns `false`. The platform calls every applicable
 * customizer against the *same* builder in registration order, but stops at the first one that
 * returns `true` - so `true` doesn't mean "I updated the text", it means "stop, don't let anyone
 * else touch this node" (e.g. Spring Boot's own customizer, which renders `[devtools]` and the
 * port, returns `true` and would otherwise shut us out entirely if it ran first). Returning `false`
 * after adding our text lets the loop continue so those other customizers still get to contribute.
 */
class RunDashboardEnvironmentCustomizer : RunDashboardCustomizer() {
  override fun isApplicable(settings: RunnerAndConfigurationSettings, descriptor: RunContentDescriptor?): Boolean =
    EnvironmentInjectionSupport.isInjectionSupported(settings.configuration, EnvironmentInjectionSupport.EP_NAME.extensionList)

  override fun updatePresentation(
    customizationBuilder: RunDashboardCustomizationBuilder,
    settings: RunnerAndConfigurationSettings,
    descriptor: RunContentDescriptor?
  ): Boolean {
    val label = launchEnvironmentLabel(descriptor) ?: return false
    customizationBuilder.addText(" [$label]", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    return false
  }
}

/** The environment label [RunEnvironmentLaunchTagger] stamped on [descriptor]'s process, if any. */
fun launchEnvironmentLabel(descriptor: RunContentDescriptor?): String? =
  descriptor?.processHandler?.getUserData(RUN_ENVIRONMENT_LAUNCH_TAG)
