package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.util.Key

/**
 * The [io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo.label]
 * of whichever environment was selected at the moment a given process was launched.
 *
 * Stamped onto a [ProcessHandler] by [RunEnvironmentLaunchTagger] right as the process starts, and
 * read back by [RunDashboardEnvironmentCustomizer] to show it in the Services tree - a snapshot of the
 * selection *at launch time*, not the project's current selection, since those two can drift apart the
 * moment the user switches environments while a previous run is still alive.
 */
val RUN_ENVIRONMENT_LAUNCH_TAG: Key<String> = Key.create("io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.launchedWith")
