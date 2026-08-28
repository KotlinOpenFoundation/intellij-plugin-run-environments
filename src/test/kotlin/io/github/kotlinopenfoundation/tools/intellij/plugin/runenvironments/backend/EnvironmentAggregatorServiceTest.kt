package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class EnvironmentAggregatorServiceTest : BasePlatformTestCase() {
  fun `test dispose does nothing and does not throw`() {
    Disposer.dispose(EnvironmentAggregatorService(project))
  }
}
