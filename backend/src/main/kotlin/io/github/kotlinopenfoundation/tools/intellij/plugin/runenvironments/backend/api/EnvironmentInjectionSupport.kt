package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * Implemented by each optional injection module's own extension class (`backend-java`'s
 * `InjectEnvironmentVariablesExtension`, `backend-gradle`'s `InjectEnvironmentVariablesGradleExtension`,
 * ...) alongside whatever platform extension point actually lets it inject - so the one class is
 * both "how do I inject" and "do I apply here" instead of two separately-registered classes that
 * could drift apart.
 *
 * Lets the frontend hide the environment selector entirely for a configuration nothing supports
 * (Shell Script, Docker, npm, ...) instead of showing a selector that would silently do nothing.
 */
interface EnvironmentInjectionSupport {
  /**
   * Whether this class's own injection mechanism would actually take effect for [configuration].
   *
   * Called with the toolbar's currently *selected* run configuration whenever that selection (or
   * its settings) changes - keep it a fast, side-effect-free type/property check, not something
   * that inspects the configuration's execution state (that would require actually launching it).
   *
   * @param configuration the run configuration to check.
   * @return `true` only if this class's platform-specific injection hook is actually reached for
   *   this configuration - `false` shows no false promise: the environment selector stays hidden
   *   rather than appearing to work and silently doing nothing.
   */
  fun supports(configuration: RunConfiguration): Boolean

  companion object {
    val EP_NAME: ExtensionPointName<EnvironmentInjectionSupport> =
      ExtensionPointName.create("io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.environmentInjectionSupport")

    /** Whether any of [checkers] claims it can inject variables into [configuration]. */
    fun isInjectionSupported(configuration: RunConfiguration?, checkers: List<EnvironmentInjectionSupport>): Boolean =
      configuration != null && checkers.any { it.supports(configuration) }
  }
}
