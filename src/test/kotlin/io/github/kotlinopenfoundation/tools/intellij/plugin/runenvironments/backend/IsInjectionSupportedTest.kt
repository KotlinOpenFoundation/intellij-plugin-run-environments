package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentInjectionSupport
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsInjectionSupportedTest {
  private val fakeConfiguration = object : RunConfiguration {
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = TODO()
    override fun getName(): String = "fake"
    override fun getIcon() = null
    override fun getFactory(): ConfigurationFactory = TODO()
    override fun setName(name: String): Unit = TODO()
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = TODO()
    override fun getProject(): Project = TODO()
    override fun clone(): RunConfiguration = TODO()
  }

  private val alwaysSupports = object : EnvironmentInjectionSupport {
    override fun supports(configuration: RunConfiguration) = true
  }

  private val neverSupports = object : EnvironmentInjectionSupport {
    override fun supports(configuration: RunConfiguration) = false
  }

  @Test
  fun `returns false when there is no selected configuration`() {
    assertFalse(EnvironmentInjectionSupport.isInjectionSupported(null, listOf(alwaysSupports)))
  }

  @Test
  fun `returns false when no registered checker supports it`() {
    assertFalse(EnvironmentInjectionSupport.isInjectionSupported(fakeConfiguration, listOf(neverSupports, neverSupports)))
  }

  @Test
  fun `returns true when at least one registered checker supports it`() {
    assertTrue(EnvironmentInjectionSupport.isInjectionSupported(fakeConfiguration, listOf(neverSupports, alwaysSupports)))
  }

  @Test
  fun `returns false when there are no checkers registered at all`() {
    assertFalse(EnvironmentInjectionSupport.isInjectionSupported(fakeConfiguration, emptyList()))
  }
}
