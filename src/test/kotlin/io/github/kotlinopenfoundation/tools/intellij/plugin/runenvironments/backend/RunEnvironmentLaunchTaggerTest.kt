package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentInjectionSupport
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class RunEnvironmentLaunchTaggerTest {
  private val environments = listOf(EnvironmentInfo("dev", variables = mapOf("HOST" to "dev.example.com")))

  private val configuration = object : RunConfiguration {
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = TODO()
    override fun getName(): String = "fake"
    override fun getIcon() = null
    override fun getFactory(): ConfigurationFactory = TODO()
    override fun setName(name: String): Unit = TODO()
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = TODO()
    override fun getProject(): Project = TODO()
    override fun clone(): RunConfiguration = TODO()
  }

  private val supportsEverything = object : EnvironmentInjectionSupport {
    override fun supports(configuration: RunConfiguration) = true
  }
  private val supportsNothing = object : EnvironmentInjectionSupport {
    override fun supports(configuration: RunConfiguration) = false
  }

  @Test
  fun `returns null when the configuration is not a RunConfiguration`() {
    assertEquals(null, launchTagFor(null, environments, "dev", listOf(supportsEverything)))
  }

  @Test
  fun `returns null when no checker supports the configuration`() {
    assertEquals(null, launchTagFor(configuration, environments, "dev", listOf(supportsNothing)))
  }

  @Test
  fun `returns null when nothing is selected even if the configuration is supported`() {
    assertEquals(null, launchTagFor(configuration, environments, null, listOf(supportsEverything)))
  }

  @Test
  fun `returns the selected environment's label when the configuration is supported`() {
    assertEquals("dev", launchTagFor(configuration, environments, "dev", listOf(supportsEverything)))
  }
}
