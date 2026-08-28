package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.execution.ExecutionListener
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.dashboard.RunDashboardCustomizer
import com.intellij.execution.dashboard.RunDashboardManager
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Key
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentInjectionSupport
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Stamps [RUN_ENVIRONMENT_LAUNCH_TAG] onto every process this plugin actually injects variables
 * into, snapshotting the project's currently selected environment label at the moment the process
 * starts.
 *
 * Registered as a [ExecutionListener] project listener (see
 * `kotlinopenfoundation-intellij-plugin-run-environments.backend.xml`), so it fires for every run
 * configuration launch regardless of type - filtered down to the ones [EnvironmentInjectionSupport]
 * actually applies to, the same check the frontend uses to decide whether to show the environment
 * selector at all, so a configuration nothing injects into never gets tagged.
 *
 * Uses [processStarting] (handler exists, not yet started) rather than [processStarted] (handler
 * running) - `RunConfigurationExtension.attachToProcess`
 * looked like the obvious, race-free spot to do this instead, but empirically never fires for a
 * Spring Boot run configuration (its execution state doesn't route through the same
 * `attachExtensionsToProcess` call `JavaCommandLineState` normally does) - so this falls back to the
 * one hook guaranteed to fire for every run type.
 *
 * The Services tree never recomputes a node's [RunDashboardCustomizer]
 * output on its own just because a process started - something has to call
 * [RunDashboardManager.updateDashboard], or the tag above sits on the handler unread. Confirmed this
 * IDE build (IU-2026.2.1) blanks a "Rerun" node's console pane on its own, independently of this
 * plugin (reproduces with the plugin disabled entirely) - so the poke below isn't the cause of that,
 * and doesn't need to work around it. Waiting for the process's first actual output line (rather than
 * `startNotified`) still matters on its own merits: by the time text is rendering in the console, the
 * platform's own content attachment has necessarily already finished, so the poke can't possibly run
 * before that. The listener removes itself after the first line so it doesn't re-poke on every line.
 */
class RunEnvironmentLaunchTagger : ExecutionListener {
  override fun processStarting(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
    val label = launchTagFor(
      env.runProfile as? RunConfiguration,
      env.project.service<EnvironmentAggregatorService>().environments.value,
      env.project.service<SelectedEnvironmentService>().selectedKey,
      EnvironmentInjectionSupport.EP_NAME.extensionList
    ) ?: return
    handler.putUserData(RUN_ENVIRONMENT_LAUNCH_TAG, label)

    val alreadyPoked = AtomicBoolean(false)
    handler.addProcessListener(object : ProcessListener {
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        if (!alreadyPoked.compareAndSet(false, true)) return
        handler.removeProcessListener(this)
        RunDashboardManager.getInstance(env.project).updateDashboard(true)
      }
    })
  }
}

/**
 * The label to stamp on a just-started process's [ProcessHandler], or `null` if none applies:
 * [configuration] is `null` or unsupported by every [checkers] entry, or nothing is currently selected.
 */
fun launchTagFor(
  configuration: RunConfiguration?,
  environments: List<EnvironmentInfo>,
  selectedKey: String?,
  checkers: List<EnvironmentInjectionSupport>
): String? {
  if (!EnvironmentInjectionSupport.isInjectionSupported(configuration, checkers)) return null
  return selectedEnvironmentLabel(environments, selectedKey)
}
