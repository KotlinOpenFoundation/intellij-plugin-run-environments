package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SelectedEnvironmentVariablesProjectExtensionTest : BasePlatformTestCase() {
  fun `test returns empty map when nothing is selected`() {
    assertEquals(emptyMap<String, String>(), project.selectedEnvironmentVariables())
  }
}
