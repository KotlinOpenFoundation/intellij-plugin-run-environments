package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.shared.api.EnvironmentInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectedEnvironmentVariablesTest {
  private val environments = listOf(
    EnvironmentInfo("dev", variables = mapOf("HOST" to "dev.example.com")),
    EnvironmentInfo("prod", qualifier = "app", variables = mapOf("HOST" to "prod.example.com"))
  )

  @Test
  fun `returns empty map when nothing is selected`() {
    assertEquals(emptyMap<String, String>(), selectedEnvironmentVariables(environments, null))
  }

  @Test
  fun `returns the variables of the environment matching the selected key`() {
    assertEquals(mapOf("HOST" to "dev.example.com"), selectedEnvironmentVariables(environments, "dev"))
  }

  @Test
  fun `returns empty map when the selected key matches no known environment`() {
    assertEquals(emptyMap<String, String>(), selectedEnvironmentVariables(environments, "staging"))
  }

  @Test
  fun `matches by the qualified key when the environment has a qualifier`() {
    assertEquals(mapOf("HOST" to "prod.example.com"), selectedEnvironmentVariables(environments, "prod::app"))
  }

  @Test
  fun `label returns null when nothing is selected`() {
    assertEquals(null, selectedEnvironmentLabel(environments, null))
  }

  @Test
  fun `label returns the unqualified name of the environment matching the selected key`() {
    assertEquals("dev", selectedEnvironmentLabel(environments, "dev"))
  }

  @Test
  fun `label includes the qualifier when the environment has one`() {
    assertEquals("prod (app)", selectedEnvironmentLabel(environments, "prod::app"))
  }

  @Test
  fun `label returns null when the selected key matches no known environment`() {
    assertEquals(null, selectedEnvironmentLabel(environments, "staging"))
  }
}
