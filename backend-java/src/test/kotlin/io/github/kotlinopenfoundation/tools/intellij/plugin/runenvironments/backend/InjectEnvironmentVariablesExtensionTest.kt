package io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend

import com.intellij.execution.Executor
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentSource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

/**
 * Lives in this module's own test source set (rather than the root project's shared one) because
 * exercising the real Java run configuration classes here requires bundling `com.intellij.java`
 * into the test sandbox - doing that from the root project would make it a permanent, real
 * [io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.backend.api.EnvironmentInjectionSupport]
 * checker for every other test sharing that sandbox.
 */
class InjectEnvironmentVariablesExtensionTest : BasePlatformTestCase() {
  private val extension = InjectEnvironmentVariablesExtension()

  // This module's isolated test sandbox doesn't load the backend module's plugin.xml, so the
  // environmentSource extension point EnvironmentAggregatorService queries doesn't exist here
  // unless registered by hand - JsonEnvironmentSource itself is real, just its EP declaration isn't.
  override fun setUp() {
    super.setUp()
    val area = ApplicationManager.getApplication().extensionArea
    if (!area.hasExtensionPoint(EnvironmentSource.EP_NAME)) {
      area.registerExtensionPoint(
        EnvironmentSource.EP_NAME.name,
        EnvironmentSource::class.java.name,
        ExtensionPoint.Kind.INTERFACE,
        true
      )
    }
    EnvironmentSource.EP_NAME.point.registerExtension(JsonEnvironmentSource(), testRootDisposable)
  }

  // JsonEnvironmentSource reads plain NIO paths - project.basePath isn't backed by a real directory
  // until something creates one, so the environment file needs to land on actual disk, and cleaning
  // it up again avoids leaking into whatever test in this module runs next.
  override fun tearDown() {
    try {
      Files.deleteIfExists(Path.of(project.basePath!!).resolve("run.env.json"))
    } finally {
      super.tearDown()
    }
  }

  private val nonJavaConfiguration = object : RunConfiguration {
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = TODO()
    override fun getName(): String = "fake"
    override fun getIcon() = null
    override fun getFactory(): ConfigurationFactory = TODO()
    override fun setName(name: String): Unit = TODO()
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = TODO()
    override fun getProject(): Project = TODO()
    override fun clone(): RunConfiguration = TODO()
  }

  fun `test supports a Java run configuration`() {
    assertTrue(extension.supports(ApplicationConfiguration("test", project)))
  }

  fun `test does not support a non-Java run configuration`() {
    assertFalse(extension.supports(nonJavaConfiguration))
  }

  fun `test isApplicableFor delegates to supports`() {
    assertTrue(extension.isApplicableFor(ApplicationConfiguration("test", project)))
  }

  fun `test injects the selected environment's variables into java parameters`() {
    val projectRoot = Path.of(project.basePath!!)
    Files.createDirectories(projectRoot)
    projectRoot.resolve("run.env.json").writeText("""{"dev": {"HOST": "dev.example.com"}}""")
    project.service<SelectedEnvironmentService>().selectedKey = "dev"

    val javaParameters = JavaParameters()
    extension.updateJavaParameters(ApplicationConfiguration("test", project), javaParameters, null)

    assertEquals("dev.example.com", javaParameters.env["HOST"])
  }

  // patchCommandLine is `protected` (inherited from RunConfigurationExtension) on a `final` class,
  // so it isn't callable from outside a subclass - not covered here for that reason.
}
